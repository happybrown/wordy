package dev.brown.wordy.ui.session

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.brown.wordy.domain.session.Answer
import dev.brown.wordy.domain.session.Exercise
import dev.brown.wordy.domain.session.ExerciseGenerator
import dev.brown.wordy.ui.components.LevelTag
import dev.brown.wordy.ui.theme.masteryColors
import dev.brown.wordy.ui.components.VSpace

/**
 * 보기 버튼 색.
 *
 * 정답을 브랜드 primary 로 칠하면 아직 안 고른 보기(filledTonal, 연한 primary 계열)와
 * 색상이 같아 "무엇이 정답인지" 가 눈에 들어오지 않는다. 정답은 초록으로 따로 뺀다.
 * 초록/빨강만으로 구분하지 않도록 정답·오답 모두 글자가 흰색으로 반전돼 명도 차이도 함께 준다.
 */
@Composable
private fun optionColors(index: Int, chosen: Int?, correct: Int, answered: Boolean): androidx.compose.material3.ButtonColors {
    val cs = MaterialTheme.colorScheme
    return when {
        answered && index == correct ->
            ButtonDefaults.buttonColors(containerColor = masteryColors[4], contentColor = Color.White)
        answered && index == chosen ->
            ButtonDefaults.buttonColors(containerColor = cs.error, contentColor = cs.onError)
        // 고르지 않은 채 넘어간 보기는 뒤로 물러나게 한다
        answered -> ButtonDefaults.buttonColors(
            containerColor = cs.surfaceContainerHigh,
            contentColor = cs.onSurfaceVariant,
        )
        else -> ButtonDefaults.buttonColors(
            containerColor = cs.surfaceContainerHigh,
            contentColor = cs.onSurface,
        )
    }
}

@Composable
private fun ChoiceList(options: List<String>, correctIndex: Int, answered: Boolean, onChoose: (Int) -> Unit) {
    var chosen by remember { mutableStateOf<Int?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { i, text ->
            // 정답이 밝혀질 때 색이 튀지 않고 번지도록 한다
            val colors = optionColors(i, chosen, correctIndex, answered)
            val container by animateColorAsState(colors.containerColor, label = "optionBg")
            val content by animateColorAsState(colors.contentColor, label = "optionFg")
            Button(
                onClick = { if (!answered && chosen == null) { chosen = i; onChoose(i) } },
                enabled = !answered || i == correctIndex || i == chosen,
                colors = ButtonDefaults.buttonColors(
                    containerColor = container,
                    contentColor = content,
                    disabledContainerColor = container,
                    disabledContentColor = content,
                ),
                shape = MaterialTheme.shapes.medium,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun MeaningChoicePane(ex: Exercise.MeaningChoice, feedback: Feedback?, onSubmit: (Answer) -> Unit, onSpeak: (String) -> Unit) {
    var showHint by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ex.word.headword, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onSpeak(ex.word.headword) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "발음") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(ex.sense.pos.ko, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ex.word.ipa?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            LevelTag(ex.word)
        }
        if (ex.word.senses.size > 1) Text("의미 ${ex.sense.ord + 1}/${ex.word.senses.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        val hint = ex.sense.examples.firstOrNull()
        if (hint != null) {
            if (showHint || feedback != null) {
                val blanked = ExerciseGenerator.blankOut(hint.en, hint.focus, ex.word.allForms)?.first ?: hint.en
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainer) { Text(blanked, Modifier.padding(10.dp), style = MaterialTheme.typography.bodyLarge) }
            } else TextButton(onClick = { showHint = true }) { Text("예문 힌트 보기") }
        }
        VSpace(4)
        ChoiceList(ex.options, ex.correctIndex, answered = feedback != null) { onSubmit(Answer.Choice(it)) }
    }
}

@Composable
fun ClozePane(ex: Exercise.Cloze, feedback: Feedback?, onSubmit: (Answer) -> Unit, onSpeak: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("빈칸에 들어갈 말은?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (feedback == null) ex.blanked else ex.example.en, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(ex.example.ko, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (feedback != null) TextButton(onClick = { onSpeak(ex.example.en) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, null); Text(" 문장 듣기") }
            }
        }
        Text("${ex.sense.pos.ko} · ${ex.sense.ko}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        if (ex.options != null) ChoiceList(ex.options, ex.correctIndex, answered = feedback != null) { onSubmit(Answer.Choice(it)) }
        else TypingInput(answered = feedback != null, hintText = ex.answer.take(1), onSubmit = { text, hint -> onSubmit(Answer.Text(text, hint)) })
    }
}

@Composable
fun RecallPane(ex: Exercise.Recall, feedback: Feedback?, onSubmit: (Answer) -> Unit, onSpeak: (String) -> Unit) {
    if (ex.listen) LaunchedEffect(ex.card.id) { onSpeak(ex.word.headword) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (ex.listen) {
            Text("들리는 단어를 영어로 쓰세요", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { onSpeak(ex.word.headword) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, null); Text(" 다시 듣기") }
                OutlinedButton(onClick = { onSpeak(ex.word.primarySense.examples.first().en) }) { Text("예문으로 듣기") }
            }
            if (feedback != null) Text(ex.glosses.joinToString(" · "), style = MaterialTheme.typography.bodyLarge)
        } else {
            Text("이 뜻의 영어 단어는?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ex.glosses.forEach { Text(it, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
                    Text(ex.definition, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        TypingInput(answered = feedback != null, hintText = ex.word.headword.take(1), onSubmit = { text, hint -> onSubmit(Answer.Text(text, hint)) })
    }
}

@Composable
private fun TypingInput(answered: Boolean, hintText: String, onSubmit: (String, Boolean) -> Unit) {
    var text by remember { mutableStateOf("") }
    var usedHint by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    fun submit() { if (!answered && text.isNotBlank()) onSubmit(text, usedHint) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text, onValueChange = { if (!answered) text = it }, singleLine = true, enabled = !answered,
            placeholder = { Text(if (usedHint) "$hintText…" else "영어로 입력") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { submit() }, enabled = !answered && text.isNotBlank(), modifier = Modifier.weight(1f)) { Text("확인") }
            OutlinedButton(onClick = { usedHint = true; if (text.isEmpty()) text = hintText }, enabled = !answered && !usedHint) { Text("첫 글자 힌트") }
        }
    }
}

@Composable
fun CollocationMatchPane(ex: Exercise.CollocationMatch, feedback: Feedback?, onSubmit: (Answer) -> Unit) {
    val rights = remember { ex.pairs.map { it.second }.shuffled() }
    val matched = remember { mutableStateListOf<Pair<Int, Int>>() } // leftIndex to rightIndex
    var selectedLeft by remember { mutableStateOf<Int?>(null) }
    var selectedRight by remember { mutableStateOf<Int?>(null) }
    fun tryPair() {
        val l = selectedLeft; val r = selectedRight
        if (l != null && r != null) { matched += l to r; selectedLeft = null; selectedRight = null }
        if (matched.size == ex.pairs.size && feedback == null) {
            val mismatches = matched.count { (li, ri) -> ex.pairs[li].second != rights[ri] }
            onSubmit(Answer.Pairs(mismatches))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("빈칸과 어울리는 단어를 짝지어 보세요 (왼쪽 → 오른쪽 순서로 탭)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ex.pairs.forEachIndexed { i, (left, right) ->
                    val m = matched.firstOrNull { it.first == i }
                    val ok = m != null && rights[m.second] == right
                    val color = when { m == null -> null; feedback != null || m != null -> if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer; else -> null }
                    FilterChip(selected = selectedLeft == i, onClick = { if (m == null && feedback == null) { selectedLeft = i; tryPair() } },
                        label = { Text(if (m != null) left.replace(ExerciseGenerator.BLANK, rights[m.second]) else left) },
                        colors = if (color != null) androidx.compose.material3.FilterChipDefaults.filterChipColors(containerColor = color) else androidx.compose.material3.FilterChipDefaults.filterChipColors(),
                        modifier = Modifier.fillMaxWidth())
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rights.forEachIndexed { i, r ->
                    val used = matched.any { it.second == i }
                    FilterChip(selected = selectedRight == i, enabled = !used && feedback == null, onClick = { selectedRight = i; tryPair() }, label = { Text(r) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

    }
}

@Composable
fun SentenceScramblePane(ex: Exercise.SentenceScramble, feedback: Feedback?, onSubmit: (Answer) -> Unit) {
    val pool = remember { mutableStateListOf(*ex.shuffled.toTypedArray()) }
    val built = remember { mutableStateListOf<String>() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("단어 조각을 순서대로 눌러 문장을 완성하세요", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(ex.example.ko, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            FlowRow(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                built.forEachIndexed { i, chunk ->
                    FilterChip(selected = true, enabled = feedback == null, onClick = { built.removeAt(i); pool += chunk }, label = { Text(chunk) })
                }
                if (built.isEmpty()) Text("여기에 문장이 만들어져요", color = MaterialTheme.colorScheme.outline)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            pool.forEachIndexed { i, chunk -> FilterChip(selected = false, enabled = feedback == null, onClick = { pool.removeAt(i); built += chunk }, label = { Text(chunk) }) }
        }
        Button(onClick = { onSubmit(Answer.Order(built.toList())) }, enabled = feedback == null && pool.isEmpty(), modifier = Modifier.fillMaxWidth()) { Text("확인") }
        if (feedback != null) Text(ex.example.en, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}
