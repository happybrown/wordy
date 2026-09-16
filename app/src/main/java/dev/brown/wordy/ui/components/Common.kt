package dev.brown.wordy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.brown.wordy.domain.model.Mastery
import dev.brown.wordy.ui.theme.masteryColors

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
            .background(masteryColors[mastery.level])
            .then(if (describe) Modifier.semantics { contentDescription = mastery.ko } else Modifier)
    )
}

@Composable
fun MasteryChip(mastery: Mastery, modifier: Modifier = Modifier) {
    Surface(modifier, shape = CircleShape, color = masteryColors[mastery.level].copy(alpha = 0.18f)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MasteryDot(mastery, size = 8)
            Text(mastery.ko, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * 섹션 제목. 색을 빼고 굵기·크기로만 구분한다.
 * 파란 소제목이 화면마다 반복되면 정작 강조해야 할 버튼과 경쟁한다.
 */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.padding(top = 24.dp, bottom = 10.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/**
 * 숫자 타일. 값이 주인공이라 크게 두고 라벨은 작게 받친다.
 * [accent] 를 주면 값에만 색이 들어가 한 타일을 도드라지게 할 수 있다.
 */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, accent: Color? = null) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent ?: MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

/**
 * 진행 막대. 값이 바뀔 때 튀지 않고 흘러가도록 애니메이션을 준다.
 * 양 끝을 둥글게 깎아 얇아도 또렷하게 보인다.
 */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Int = 10,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 220f),
        label = "progress",
    )
    val barColor by animateColorAsState(color, label = "progressColor")
    Box(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        if (animated > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

@Composable
fun VSpace(dp: Int) = Spacer(Modifier.height(dp.dp))
