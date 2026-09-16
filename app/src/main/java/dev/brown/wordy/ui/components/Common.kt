package dev.brown.wordy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.brown.wordy.domain.model.Mastery
import dev.brown.wordy.ui.theme.MasteryColors

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * 숙달 단계 점. 색으로만 단계를 나타내므로 옆에 글자가 없는 자리에서는
 * [describe] 를 켜서 스크린 리더가 읽을 수 있게 한다.
 */
@Composable
fun MasteryDot(mastery: Mastery, modifier: Modifier = Modifier, size: Int = 10, describe: Boolean = false) {
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MasteryColors[mastery.level])
            .then(if (describe) Modifier.semantics { contentDescription = mastery.ko } else Modifier)
    )
}

@Composable
fun MasteryChip(mastery: Mastery, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.small, color = MasteryColors[mastery.level].copy(alpha = 0.18f)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MasteryDot(mastery, size = 8)
            Text(mastery.ko, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(top = 12.dp, bottom = 6.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(8.dp).background(color))
    }
}

@Composable
fun VSpace(dp: Int) = Spacer(Modifier.height(dp.dp))
