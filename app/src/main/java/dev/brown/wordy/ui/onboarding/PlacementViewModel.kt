package dev.brown.wordy.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brown.wordy.di.AppContainer
import dev.brown.wordy.domain.model.Level
import dev.brown.wordy.domain.model.Word
import dev.brown.wordy.domain.placement.Placement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/** 진단 문제 한 개. 뜻 고르기만 쓴다 — 타이핑은 느리고 30문항을 끝까지 못 가게 만든다. */
data class PlacementQuestion(
    val level: Level,
    val headword: String,
    val pos: String,
    val options: List<String>,
    val correctIndex: Int,
)

data class PlacementUiState(
    val loading: Boolean = true,
    val questions: List<PlacementQuestion> = emptyList(),
    val index: Int = 0,
    /** 사용자가 고른 보기. 아직 안 고른 문제는 null. */
    val answers: List<Int?> = emptyList(),
    val finished: Boolean = false,
    val recommended: Level = Level.ELEM,
    val summary: String = "",
) {
    val current: PlacementQuestion? get() = questions.getOrNull(index)
    val total: Int get() = questions.size
}

/**
 * 레벨 진단. 레벨마다 [PER_LEVEL]문제씩, 쉬운 레벨부터 낸다.
 * 어려운 레벨부터 내면 처음부터 막혀서 그만두기 쉽다.
 */
class PlacementViewModel(private val c: AppContainer, private val random: Random = Random.Default) : ViewModel() {
    private val _state = MutableStateFlow(PlacementUiState())
    val state: StateFlow<PlacementUiState> = _state

    init {
        viewModelScope.launch {
            val qs = Level.entries.flatMap { buildFor(it) }
            _state.value = PlacementUiState(loading = false, questions = qs, answers = List(qs.size) { null })
        }
    }

    /**
     * 한 레벨의 문제를 만든다. 그 레벨에서 무작위로 단어를 뽑되, 뜻이 겹치는 오답이 나오면
     * 문제가 성립하지 않으므로 보기가 4개 모이지 않는 단어는 건너뛴다.
     */
    private suspend fun buildFor(level: Level): List<PlacementQuestion> {
        val ids = c.words.wordIdsByLevel(level)
        if (ids.isEmpty()) return emptyList()
        // 빈도순 앞쪽이 그 레벨의 대표 단어다. 뒤쪽 희귀어로 진단하면 실제보다 낮게 나온다.
        val pool = ids.take((ids.size * 0.6).toInt().coerceAtLeast(PER_LEVEL * 4)).shuffled(random)
        val out = mutableListOf<PlacementQuestion>()
        for (id in pool) {
            if (out.size >= PER_LEVEL) break
            val w = c.words.word(id) ?: continue
            out += question(w) ?: continue
        }
        return out
    }

    private suspend fun question(w: Word): PlacementQuestion? {
        val sense = w.primarySense
        val correct = sense.ko
        val distractors = c.words.distractorSenses(w, sense.pos, 6)
            .map { it.ko }
            .filter { it != correct && w.senses.none { s -> s.ko == it } }
            .distinct()
            .take(3)
        if (distractors.size < 3) return null
        val options = (distractors + correct).shuffled(random)
        return PlacementQuestion(w.level, w.headword, sense.pos.ko, options, options.indexOf(correct))
    }

    fun answer(optionIndex: Int) {
        val s = _state.value
        if (s.current == null) return
        val answers = s.answers.toMutableList().also { it[s.index] = optionIndex }
        val nextIndex = s.index + 1

        if (nextIndex >= s.questions.size) {
            val correctBy = mutableMapOf<Level, Int>()
            val askedBy = mutableMapOf<Level, Int>()
            s.questions.forEachIndexed { i, question ->
                askedBy[question.level] = (askedBy[question.level] ?: 0) + 1
                if (answers[i] == question.correctIndex) correctBy[question.level] = (correctBy[question.level] ?: 0) + 1
            }
            val rec = Placement.recommend(correctBy, askedBy)
            _state.value = s.copy(
                answers = answers, index = nextIndex, finished = true,
                recommended = rec, summary = Placement.summary(correctBy, askedBy, rec),
            )
        } else {
            _state.value = s.copy(answers = answers, index = nextIndex)
        }
    }

    companion object {
        /** 레벨당 문항 수. 5레벨 × 6 = 30문항, 계획서 기준. */
        const val PER_LEVEL = 6
    }
}
