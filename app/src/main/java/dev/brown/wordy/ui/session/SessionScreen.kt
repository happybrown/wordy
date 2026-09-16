package dev.brown.wordy.ui.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
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
import dev.brown.wordy.ui.components.ProgressBar
import dev.brown.wordy.ui.components.StatTile
import dev.brown.wordy.ui.components.VSpace
import dev.brown.wordy.ui.components.WordCard
import dev.brown.wordy.ui.components.appViewModel
import dev.brown.wordy.ui.theme.bloomGradient
import dev.brown.wordy.ui.theme.onBloomGradient

@Composable
fun SessionScreen(extraNew: Int, onExit: () -> Unit, onOpenWord: (String) -> Unit) {
    val vm = appViewModel(key = "session-$extraNew") { SessionViewModel(it, extraNew) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        val progress = when (val s = state) { is SessionUiState.Learn -> s.progress; is SessionUiState.Quiz -> s.progress; else -> null }
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) { Icon(Icons.Default.Close, "나가기") }
            if (progress != null) {
                ProgressBar(
                    if (progress.total == 0) 0f else progress.done.toFloat() / progress.total,
                    modifier = Modifier.weight(1f),
                    height = 8,
                )
                Text(
                    "${progress.done}/${progress.total}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp),
                )
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
        Surface(color = MaterialTheme.colorScheme.background) {
            Button(
                onClick = onLearned,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp).height(52.dp),
                shape = CircleShape,
            ) { Text("확인했어요  →  바로 퀴즈", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
fun QuizPane(exercise: Exercise, feedback: Feedback?, onSubmit: (Answer) -> Unit, onContinue: () -> Unit, onSpeak: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TypeChip(exercise.type.ko)
                if (exercise.word.isCore) TypeChip("Core", accent = true)
            }
            VSpace(14)
            when (exercise) {
                is Exercise.MeaningChoice -> MeaningChoicePane(exercise, feedback, onSubmit, onSpeak)
                is Exercise.Cloze -> ClozePane(exercise, feedback, onSubmit, onSpeak)
                is Exercise.Recall -> RecallPane(exercise, feedback, onSubmit, onSpeak)
                is Exercise.CollocationMatch -> CollocationMatchPane(exercise, feedback, onSubmit)
                is Exercise.SentenceScramble -> SentenceScramblePane(exercise, feedback, onSubmit)
            }
            VSpace(16)
        }
        AnimatedVisibility(
            visible = feedback != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = fadeOut(),
        ) {
            feedback?.let { FeedbackPanel(it, onContinue, onSpeak) }
        }
    }
}

@Composable
private fun FeedbackPanel(f: Feedback, onContinue: () -> Unit, onSpeak: (String) -> Unit) {
    val good = f.correct
    val container = when { good -> MaterialTheme.colorScheme.primaryContainer; f.almost -> MaterialTheme.colorScheme.tertiaryContainer; else -> MaterialTheme.colorScheme.errorContainer }
    Surface(
        color = container,
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(52.dp), shape = CircleShape) { Text("계속", style = MaterialTheme.typography.titleMedium) }
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
    val accuracy = if (stats.reviewed == 0) null else stats.correct * 100 / stats.reviewed
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        VSpace(32)
        // 끝냈다는 사실 자체가 보상이 되도록 큰 원형 표시를 둔다
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(bloomGradient)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = onBloomGradient,
                modifier = Modifier.size(46.dp),
            )
        }
        VSpace(18)
        Text(
            "오늘 세션 끝!",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        VSpace(6)
        Text(
            if (accuracy != null && accuracy >= 90) "정확도가 아주 높아요. 이 속도면 금방 늡니다."
            else "꾸준함이 제일 빠른 길이에요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        VSpace(28)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("새 단어", "${stats.newLearned}", Modifier.weight(1f))
            StatTile("복습", "${stats.reviewed}", Modifier.weight(1f))
            StatTile(
                "정답률",
                accuracy?.let { "$it%" } ?: "-",
                Modifier.weight(1f),
                accent = if (accuracy != null && accuracy >= 80) MaterialTheme.colorScheme.primary else null,
            )
        }

        if (stats.newlyFamiliar.isNotEmpty()) {
            VSpace(28)
            Text("새로 '익숙' 단계에 오른 단어", style = MaterialTheme.typography.titleMedium)
            VSpace(10)
            stats.newlyFamiliar.forEach { w ->
                Surface(
                    onClick = { onOpenWord(w.id) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(w.headword, style = MaterialTheme.typography.titleSmall)
                        Text(w.primarySense.ko, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        VSpace(24)
        Text(
            "내일 예정 복습 ${stats.dueTomorrow}개",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        VSpace(20)
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth().height(54.dp), shape = CircleShape) {
            Text("홈으로", style = MaterialTheme.typography.titleMedium)
        }
        VSpace(32)
    }
}

/** 문제 유형 표시. 파란 글자 한 줄보다 칩이 "지금 무슨 문제인지"를 빨리 알려 준다. */
@Composable
private fun TypeChip(text: String, accent: Boolean = false) {
    Surface(
        shape = CircleShape,
        color = if (accent) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (accent) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
