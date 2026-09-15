package dev.brown.wordy.ui.practice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.brown.wordy.di.AppContainer
import dev.brown.wordy.domain.model.Card
import dev.brown.wordy.domain.model.Skill
import dev.brown.wordy.domain.session.Answer
import dev.brown.wordy.domain.session.AnswerEvaluator
import dev.brown.wordy.domain.session.Exercise
import dev.brown.wordy.domain.srs.CardState
import dev.brown.wordy.ui.components.EmptyMessage
import dev.brown.wordy.ui.components.LoadingBox
import dev.brown.wordy.ui.components.appViewModel
import dev.brown.wordy.ui.session.Feedback
import dev.brown.wordy.ui.session.QuizPane
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** 한 단어의 모든 문제 유형을 스케줄에 영향 없이 풀어 보는 연습 모드 */
sealed interface PracticeState {
    data object Loading : PracticeState
    data class Quiz(val exercise: Exercise, val feedback: Feedback?, val index: Int, val total: Int) : PracticeState
    data class Done(val correct: Int, val total: Int) : PracticeState
    data class Empty(val message: String) : PracticeState
}

class PracticeViewModel(private val c: AppContainer, private val wordId: String) : ViewModel() {
    private val _state = MutableStateFlow<PracticeState>(PracticeState.Loading)
    val state: StateFlow<PracticeState> = _state
    private val exercises = ArrayList<Exercise>()
    private var index = 0
    private var correct = 0
    private var shownAt = 0L

    init {
        viewModelScope.launch {
            val word = c.words.word(wordId)
            if (word == null) { _state.value = PracticeState.Empty("단어를 찾을 수 없어요."); return@launch }
            val now = c.now()
            fun card(skill: Skill, senseId: String?, reps: Int = 0, state: CardState = CardState.NEW, stability: Double = 0.0) =
                Card(-1, word.id, senseId, skill, state, now, stability, 5.0, reps, 0, null, now)
            val tts = c.tts.available.value
            word.senses.forEach { s -> exercises += c.exercises.meaningChoice(card(Skill.RECOGNIZE, s.id), word) }
            exercises += c.exercises.cloze(card(Skill.CONTEXT, word.primarySense.id), word, typing = false)
            exercises += c.exercises.cloze(card(Skill.CONTEXT, word.senses.getOrNull(1)?.id ?: word.primarySense.id), word, typing = true)
            exercises += c.exercises.recall(card(Skill.RECALL, null), word, listen = false)
            if (tts) exercises += c.exercises.recall(card(Skill.RECALL, null, reps = 1), word, listen = true)
            if (word.allCollocations.size >= 2) c.exercises.collocationMatch(card(Skill.USE, null), word)?.let { exercises += it }
            exercises += c.exercises.sentenceScramble(card(Skill.USE, word.primarySense.id), word)
            show()
        }
    }

    private fun show() {
        if (index >= exercises.size) { _state.value = PracticeState.Done(correct, exercises.size); return }
        shownAt = c.now()
        _state.value = PracticeState.Quiz(exercises[index], null, index, exercises.size)
    }

    fun submit(answer: Answer) {
        val st = _state.value as? PracticeState.Quiz ?: return
        if (st.feedback != null) return
        val ex = st.exercise
        val outcome = AnswerEvaluator.evaluate(ex, answer, c.now() - shownAt)
        if (outcome.correct) correct++
        val sense = ex.card.senseId?.let { id -> ex.word.senses.firstOrNull { it.id == id } }
        _state.value = st.copy(feedback = Feedback(outcome.correct, outcome.almost, null, null, ex.word, sense, AnswerEvaluator.correctAnswerOf(ex), null))
    }

    fun next() { index++; show() }
    fun speak(text: String) = c.tts.speak(text)
}

@Composable
fun PracticeScreen(wordId: String, onExit: () -> Unit) {
    val vm = appViewModel(key = "practice-$wordId") { PracticeViewModel(it, wordId) }
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) { Icon(Icons.Default.Close, "나가기") }
            val q = state as? PracticeState.Quiz
            if (q != null) {
                LinearProgressIndicator(progress = { q.index.toFloat() / q.total }, modifier = Modifier.weight(1f).height(6.dp))
                Text("  연습 ${q.index + 1}/${q.total}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 8.dp))
            } else Box(Modifier.weight(1f))
        }
        when (val s = state) {
            PracticeState.Loading -> LoadingBox()
            is PracticeState.Empty -> EmptyMessage(s.message)
            is PracticeState.Quiz -> key(s.index) { QuizPane(s.exercise, s.feedback, onSubmit = vm::submit, onContinue = vm::next, onSpeak = vm::speak) }
            is PracticeState.Done -> Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyMessage("연습 끝! ${s.total}문제 중 ${s.correct}개 정답.\n연습은 복습 일정에 반영되지 않아요.", Modifier.weight(1f))
                Button(onClick = onExit, Modifier.fillMaxWidth()) { Text("단어로 돌아가기") }
            }
        }
    }
}
