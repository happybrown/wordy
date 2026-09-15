package dev.brown.wordy.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brown.wordy.data.repo.ProgressRepository
import dev.brown.wordy.di.AppContainer
import dev.brown.wordy.domain.model.Card
import dev.brown.wordy.domain.model.Mastery
import dev.brown.wordy.domain.model.Sense
import dev.brown.wordy.domain.model.Skill
import dev.brown.wordy.domain.model.Word
import dev.brown.wordy.domain.session.Answer
import dev.brown.wordy.domain.session.AnswerEvaluator
import dev.brown.wordy.domain.session.Exercise
import dev.brown.wordy.domain.srs.Rating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class Feedback(
    val correct: Boolean, val almost: Boolean, val rating: Rating?, val nextDays: Double?,
    val word: Word, val sense: Sense?, val correctAnswer: String, val mastery: Mastery?,
)

data class SessionProgress(val done: Int, val total: Int)

data class SessionStats(val newLearned: Int, val reviewed: Int, val correct: Int, val newlyFamiliar: List<Word>, val dueTomorrow: Int)

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data class Empty(val message: String) : SessionUiState
    data class Learn(val word: Word, val progress: SessionProgress) : SessionUiState
    data class Quiz(val exercise: Exercise, val feedback: Feedback?, val progress: SessionProgress) : SessionUiState
    data class Summary(val stats: SessionStats) : SessionUiState
}

private sealed interface Item {
    data class Learn(val wordId: String) : Item
    data class Review(val card: Card) : Item
}

class SessionViewModel(private val c: AppContainer, private val extraNew: Int) : ViewModel() {
    private val _state = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val state: StateFlow<SessionUiState> = _state
    val ttsAvailable get() = c.tts.available

    private val queue = ArrayDeque<Item>()
    private var total = 0
    private var done = 0
    private var shownAt = 0L
    private val reinserts = HashMap<Long, Int>()
    private var newLearned = 0; private var reviewed = 0; private var correct = 0
    private val familiarBefore = HashSet<String>()
    private val newlyFamiliar = LinkedHashMap<String, Word>()
    private var lastType: Map<Long, String> = emptyMap()

    init { viewModelScope.launch { start() } }

    private suspend fun start() {
        val plan = c.sessionBuilder.build(c.now(), extraNew = extraNew)
        if (plan.isEmpty) { _state.value = SessionUiState.Empty("지금은 복습할 카드도, 오늘 배울 새 단어도 없어요.\n홈에서 '더 배우기'를 눌러 이어갈 수 있어요."); return }
        val reviews = plan.dueCards.map { Item.Review(it) }
        val learns = plan.newWordIds.map { Item.Learn(it) }
        // 복습 3개마다 새 단어 1개를 끼워 넣는다 (복습이 없으면 새 단어만)
        val merged = ArrayList<Item>()
        var li = 0
        reviews.forEachIndexed { i, r -> merged += r; if ((i + 1) % 3 == 0 && li < learns.size) merged += learns[li++] }
        while (li < learns.size) merged += learns[li++]
        queue.addAll(merged); total = merged.size
        val statuses = c.progress.statuses(plan.dueCards.map { it.wordId }.distinct() + plan.newWordIds)
        familiarBefore += statuses.filter { it.value.mastery.level >= Mastery.FAMILIAR.level }.keys
        advance()
    }

    private fun progress() = SessionProgress(done, total)

    private suspend fun advance() {
        val item = queue.removeFirstOrNull()
        if (item == null) { finish(); return }
        when (item) {
            is Item.Learn -> {
                val word = c.words.word(item.wordId)
                if (word == null) { advance(); return }
                _state.value = SessionUiState.Learn(word, progress())
            }
            is Item.Review -> {
                val word = c.words.word(item.card.wordId)
                if (word == null) { advance(); return }
                val preferVariant = lastType[item.card.id] != null
                val exercise = c.exercises.generate(item.card, word, ttsAvailable.value, preferVariant)
                shownAt = c.now()
                _state.value = SessionUiState.Quiz(exercise, null, progress())
            }
        }
    }

    /** 학습 카드 확인 → 카드 생성 → 바로 인식 퀴즈, 잠시 후 문맥 퀴즈 */
    fun onLearned() {
        val st = _state.value as? SessionUiState.Learn ?: return
        viewModelScope.launch {
            val cards = c.engine.introduce(st.word, c.now())
            newLearned++; done++
            val first = st.word.primarySense.id
            cards.firstOrNull { it.skill == Skill.RECOGNIZE && it.senseId == first }?.let { queue.addFirst(Item.Review(it)); total++ }
            cards.firstOrNull { it.skill == Skill.CONTEXT && it.senseId == first }?.let { queue.add(minOf(4, queue.size), Item.Review(it)); total++ }
            advance()
        }
    }

    fun submit(answer: Answer) {
        val st = _state.value as? SessionUiState.Quiz ?: return
        if (st.feedback != null) return
        val ex = st.exercise
        val elapsed = (c.now() - shownAt).coerceAtLeast(0)
        viewModelScope.launch {
            val finalOutcome = AnswerEvaluator.evaluate(ex, answer, elapsed, strict = c.settings.current().strictTyping)
            val result = c.engine.answer(ex, finalOutcome, c.now())
            reviewed++; done++; if (finalOutcome.correct) correct++
            lastType = lastType + (ex.card.id to ex.type.name)
            if (result.mastery.level >= Mastery.FAMILIAR.level && ex.word.id !in familiarBefore) newlyFamiliar[ex.word.id] = ex.word
            // 학습 단계(수 분 뒤 due)나 오답은 같은 세션 안에서 다시 본다 (카드당 최대 3회)
            val soon = result.card.due <= c.now() + 20 * 60_000L
            val n = reinserts.getOrDefault(ex.card.id, 0)
            if (soon && n < 3) { reinserts[ex.card.id] = n + 1; queue.add(minOf(3, queue.size), Item.Review(result.card)); total++ }
            val sense = ex.card.senseId?.let { id -> ex.word.senses.firstOrNull { it.id == id } }
            _state.value = st.copy(feedback = Feedback(finalOutcome.correct, finalOutcome.almost, result.rating, result.scheduledDays, ex.word, sense, AnswerEvaluator.correctAnswerOf(ex), result.mastery), progress = progress())
        }
    }

    fun onContinue() { viewModelScope.launch { advance() } }

    fun speak(text: String) = c.tts.speak(text)

    private suspend fun finish() {
        val tomorrowEnd = ProgressRepository.startOfDay(LocalDate.now().plusDays(2))
        val dueTomorrow = c.progress.dueBetween(c.now(), tomorrowEnd)
        _state.value = SessionUiState.Summary(SessionStats(newLearned, reviewed, correct, newlyFamiliar.values.toList(), dueTomorrow))
    }
}
