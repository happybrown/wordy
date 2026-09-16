package dev.brown.wordy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.brown.wordy.domain.model.Example
import dev.brown.wordy.domain.model.Sense
import dev.brown.wordy.domain.model.Word
import dev.brown.wordy.ui.theme.masteryColors

/** 예문에서 focus 구간을 강조한 텍스트 */
@Composable
fun focusedExample(example: Example, color: androidx.compose.ui.graphics.Color) = buildAnnotatedString {
    val i = example.en.indexOf(example.focus)
    if (i < 0) { append(example.en); return@buildAnnotatedString }
    append(example.en.substring(0, i))
    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) { append(example.focus) }
    append(example.en.substring(i + example.focus.length))
}

@Composable
fun ExampleRow(example: Example, onSpeak: ((String) -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(focusedExample(example, MaterialTheme.colorScheme.primary), style = MaterialTheme.typography.bodyLarge)
            Text(example.ko, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onSpeak != null) IconButton(onClick = { onSpeak(example.en) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "듣기") }
    }
}

@Composable
fun WordHeader(word: Word, onSpeak: ((String) -> Unit)?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(word.headword, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            if (onSpeak != null) IconButton(onClick = { onSpeak(word.headword) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "발음 듣기") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            word.ipa?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            LevelTag(word)
        }
    }
}

@Composable
fun LevelTag(word: Word) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(word.level.ko, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
        }
        if (word.isCore) Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.tertiaryContainer) {
            Text("Core", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SenseBlock(sense: Sense, index: Int, total: Int, onSpeak: ((String) -> Unit)?, modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (total > 1) Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(sense.pos.ko, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(sense.ko, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        Text(sense.en, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
        sense.pattern?.let { Text("문형  $it", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary) }
        sense.nuance?.let {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text(it, Modifier.padding(10.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        (if (compact) sense.examples.take(1) else sense.examples).forEach { ExampleRow(it, onSpeak) }
        if (sense.collocations.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                Text("연어", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                FlowChips(sense.collocations)
            }
        }
        if (!compact && sense.phrasals.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                Text("구동사", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                sense.phrasals.forEach { p ->
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(p.phrase, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text(p.ko, style = MaterialTheme.typography.bodyMedium)
                        }
                        ExampleRow(p.example, onSpeak)
                    }
                }
            }
        }
    }
}

@Composable
fun FlowChips(items: List<String>) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        items.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
    }
}

/** 단어 전체 카드 (학습 도입 화면·단어 상세에서 공용) */
@Composable
fun WordCard(word: Word, onSpeak: ((String) -> Unit)?, modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        WordHeader(word, onSpeak)
        word.senses.forEachIndexed { i, s ->
            if (i > 0) HorizontalDivider()
            SenseBlock(s, i, word.senses.size, onSpeak, compact = compact)
        }
        if (word.confusables.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("헷갈리는 단어", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    word.confusables.forEach { c -> Text(buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(c.word) }; append("  ${c.note}") }, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        if (word.family.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("파생어", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                FlowChips(word.family.map { "${it.form} (${it.pos}) ${it.ko}" })
            }
        }
        word.memoryHook?.let {
            Text("💡 $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
        }
        if (word.forms.isNotEmpty()) Text("변화형  ${word.forms.joinToString(", ")}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
    }
}
