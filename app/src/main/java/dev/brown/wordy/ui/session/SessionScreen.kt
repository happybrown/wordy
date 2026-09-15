package dev.brown.wordy.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.brown.wordy.domain.model.Word
import dev.brown.wordy.domain.session.Answer
import dev.brown.wordy.domain.session.Exercise
import dev.brown.wordy.domain.srs.Rating
import dev.brown.wordy.ui.components.EmptyMessage
import dev.brown.wordy.ui.components.ExampleRow
import dev.brown.wordy.ui.components.LoadingBox
import dev.brown.wordy.ui.components.MasteryChip
import dev.brown.wordy.ui.components.StatTile
import dev.brown.wordy.ui.components.VSpace
import dev.brown.wordy.ui.components.WordCard
import dev.brown.wordy.ui.components.appViewModel

@Composable
fun SessionScreen(extraNew: Int, onExit: () -> Unit, onOpenWord: (String) -> Unit) {
    val vm = appViewModel(key = "session-$extraNew") { SessionViewModel(it, extraNew) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        val progress = when (val s = state) { is SessionUiState.Learn -> s.progress; is SessionUiState.Quiz -> s.progress; else -> null }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) { Icon(Icons.Default.Close, "나가기") }
            if (progress != null) {
                LinearProgressIndicator(progress = { if (progress.total == 0) 0f else progress.done.toFloat() / progress.total }, modifier = Modifier.weight(1f).height(6.dp))
                Text("  ${progress.done}/${progress.total}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 8.dp))
            } else Box(Modifier.weight(1f))
        }
        when (val s = state) {
            SessionUiState.Loading -> LoadingBox()
            is SessionUiState.Empty -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyMessage(s.message, Modifier.weight(1f))
                Button(onClick = onExit, Modifier.padding(24.dp)) { Text("홈으로") }
            }
            is SessionUiState.Learn -> key(s.word.id) { LearnPane(s.word, onSpeak = vm::speak, onLearned = vm::onLearned) }
            is SessionUiState.Quiz -> key(s.exercise.card.id, s.exercise.type, s.progress.done) {
                QuizPane(s.exercise, s.feedback, onSubmit = vm::submit, onContinue = vm::onContinue, onSpeak = vm::speak)
            }
            is SessionUiState.Summary -> SummaryPane(s.stats, onExit, onOpenWord)
        }
    }
}

@Composable
private fun LearnPane(word: Word, onSpeak: (String) -> Unit, onLearned: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text("새 단어", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            VSpace(4)
            WordCard(word, onSpeak)
            VSpace(16)
        }
        Surface(tonalElevation = 3.dp) {
            Button(onClick = onLearned, modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)) { Text("확인했어요 → 바로 퀴즈") }
        }
    }
}

@Composable
fun QuizPane(exercise: Exercise, feedback: Feedback?, onSubmit: (Answer) -> Unit, onContinue: () -> Unit, onSpeak: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(exercise.type.ko + (if (exercise.word.isCore) "  ·  Core" else ""), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            VSpace(8)
            when (exercise) {
                is Exercise.MeaningChoice -> MeaningChoicePane(exercise, feedback, onSubmit, onSpeak)
                is Exercise.Cloze -> ClozePane(exercise, feedback, onSubmit, onSpeak)
                is Exercise.Recall -> RecallPane(exercise, feedback, onSubmit, onSpeak)
                is Exercise.CollocationMatch -> CollocationMatchPane(exercise, feedback, onSubmit)
                is Exercise.SentenceScramble -> SentenceScramblePane(exercise, feedback, onSubmit)
            }
            VSpace(16)
        }
        if (feedback != null) FeedbackPanel(feedback, onContinue, onSpeak)
    }
}

@Composable
private fun FeedbackPanel(f: Feedback, onContinue: () -> Unit, onSpeak: (String) -> Unit) {
    val good = f.correct
    val container = when { good -> MaterialTheme.colorScheme.primaryContainer; f.almost -> MaterialTheme.colorScheme.tertiaryContainer; else -> MaterialTheme.colorScheme.errorContainer }
    Surface(color = container, tonalElevation = 4.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when { good && f.rating == Rating.EASY -> "정답! 빨랐어요"; good -> "정답!"; f.almost -> "거의 맞았어요"; else -> "아쉬워요" },
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                )
                f.nextDays?.let { Text(nextText(it), style = MaterialTheme.typography.labelLarge) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(f.word.headword, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                f.sense?.let { Text("${it.pos.ko} ${it.ko}", style = MaterialTheme.typography.bodyLarge) }
                f.mastery?.let { MasteryChip(it) }
            }
            if (!good) {
                Text("정답: ${f.correctAnswer}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                f.sense?.nuance?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                f.sense?.examples?.firstOrNull()?.let { ExampleRow(it, onSpeak) }
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("계속") }
        }
    }
}

private fun nextText(days: Double): String = when {
    days < 1.0 / 24 -> "다시 ${(days * 1440).toInt().coerceAtLeast(1)}분 후"
    days < 1 -> "다시 ${(days * 24).toInt()}시간 후"
    days < 30 -> "다음 복습 ${days.toInt()}일 후"
    days < 365 -> "다음 복습 ${(days / 30).toInt()}개월 후"
    else -> "다음 복습 ${(days / 365).toInt()}년 후"
}

@Composable
private fun SummaryPane(stats: SessionStats, onExit: () -> Unit, onOpenWord: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("오늘 세션 끝!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        VSpace(16)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("새 단어", "${stats.newLearned}", Modifier.weight(1f))
            StatTile("복습", "${stats.reviewed}", Modifier.weight(1f))
            StatTile("정답률", if (stats.reviewed == 0) "-" else "${stats.correct * 100 / stats.reviewed}%", Modifier.weight(1f))
        }
        VSpace(16)
        if (stats.newlyFamiliar.isNotEmpty()) {
            Text("새로 '익숙' 단계에 오른 단어", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            VSpace(6)
            stats.newlyFamiliar.forEach { w ->
                Card(onClick = { onOpenWord(w.id) }, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Text(w.headword, fontWeight = FontWeight.SemiBold); Text(w.primarySense.ko, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            VSpace(16)
        }
        Text("내일 예정 복습 ${stats.dueTomorrow}개", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        VSpace(24)
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("홈으로") }
    }
}
