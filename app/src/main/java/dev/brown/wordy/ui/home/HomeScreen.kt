package dev.brown.wordy.ui.home

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.brown.wordy.domain.model.Level
import dev.brown.wordy.ui.components.LoadingBox
import dev.brown.wordy.ui.components.VSpace
import dev.brown.wordy.ui.components.appViewModel
import dev.brown.wordy.ui.theme.bloomGradient
import dev.brown.wordy.ui.theme.masteryColors
import dev.brown.wordy.ui.theme.onBloomGradient

@Composable
fun HomeScreen(onStartSession: (extraNew: Int) -> Unit, onOpenCore: () -> Unit, onOpenLevel: (Level) -> Unit) {
    val vm = appViewModel { HomeViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    if (s.loading) { LoadingBox(); return }

    val remainingNew = (s.dailyGoal - s.learnedToday).coerceAtLeast(0)
    val waiting = s.dueNow + remainingNew
    val nothingLeft = waiting == 0

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        VSpace(14)
        Text("안녕하세요", style = MaterialTheme.typography.headlineSmall)
        Text(
            if (s.streak > 0) "${s.streak}일 연속으로 공부하고 있어요"
            else "오늘부터 시작해 볼까요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        VSpace(14)
        HeroCard(
            waiting = waiting,
            nothingLeft = nothingLeft,
            week = s.weekActivity,
            onStart = { onStartSession(if (nothingLeft) 5 else 0) },
        )

        VSpace(12)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftTile("익힌 단어", "${s.familiarTotal}", Modifier.weight(1f))
            SoftTile("내일 복습", "${s.dueTomorrow}", Modifier.weight(1f))
        }

        VSpace(22)
        Text("핵심 회화 어휘", style = MaterialTheme.typography.titleMedium)
        VSpace(10)
        CoreCard(
            total = s.coreTotal,
            familiar = s.coreFamiliar,
            met = s.coreMet,
            onClick = onOpenCore,
        )

        VSpace(22)
        Text("레벨별 진행", style = MaterialTheme.typography.titleMedium)
        VSpace(10)
        Surface(
            Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 1.dp,
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                s.levels.forEachIndexed { i, lp ->
                    LevelRow(
                        name = lp.level.ko,
                        isCurrent = lp.level == s.currentLevel,
                        familiar = lp.familiar,
                        total = lp.total,
                        last = i == s.levels.lastIndex,
                        onClick = { onOpenLevel(lp.level) },
                    )
                }
            }
        }

        VSpace(20)
        Text(
            "단어 ${s.totalWords}개 · 누적 복습 ${s.totalReviews}회",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        VSpace(26)
    }
}

/**
 * 그라데이션 히어로. Bloom 에서 색을 쓰는 유일한 자리다.
 * 오른쪽 위 반투명 원은 그라데이션에 깊이를 주려고 겹친 것으로, 잘려 나가도 무방하다.
 */
@Composable
private fun HeroCard(waiting: Int, nothingLeft: Boolean, week: List<Int>, onStart: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
    ) {
        Box(Modifier.background(bloomGradient)) {
            // 장식 원. 카드 밖으로 넘치는 부분은 Surface 가 잘라 준다.
            Box(
                Modifier
                    .size(132.dp)
                    .offset(x = 258.dp, y = (-76).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.13f))
            )
            Column(Modifier.padding(20.dp)) {
                if (nothingLeft) {
                    Text(
                        "오늘 할 몫을 다 했어요",
                        style = MaterialTheme.typography.headlineSmall,
                        color = onBloomGradient,
                    )
                    VSpace(4)
                    Text(
                        "더 하고 싶으면 5개를 추가로 배울 수 있어요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onBloomGradient.copy(alpha = 0.82f),
                    )
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimatedCount(waiting)
                        Text(
                            "개가 기다리고 있어요",
                            style = MaterialTheme.typography.bodyLarge,
                            color = onBloomGradient.copy(alpha = 0.85f),
                            modifier = Modifier.padding(start = 6.dp, bottom = 7.dp),
                        )
                    }
                }

                VSpace(14)
                WeekBars(week)

                VSpace(14)
                // 흰 알약 버튼. 그라데이션 위에서 가장 밝아 시선이 여기로 간다.
                Surface(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = CircleShape,
                    color = Color.White,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (nothingLeft) "5개 더 배우기  →" else "지금 시작하기  →",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedCount(value: Int) {
    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 180f),
        label = "count",
    )
    Text(
        "${animated.toInt()}",
        style = MaterialTheme.typography.displayMedium,
        color = onBloomGradient,
    )
}

/** 최근 7일 학습량. 값이 아니라 리듬을 보여 주는 것이라 축이나 숫자를 넣지 않는다. */
@Composable
private fun WeekBars(week: List<Int>) {
    val max = (week.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(34.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        week.forEachIndexed { i, v ->
            val frac = v.toFloat() / max
            val isToday = i == week.lastIndex
            Box(
                Modifier
                    .weight(1f)
                    // 학습이 없는 날도 흔적은 남겨 7일 리듬이 보이게 한다
                    .fillMaxHeight(if (v > 0) frac.coerceAtLeast(0.3f) else 0.16f)
                    .clip(CircleShape)
                    .background(
                        if (v > 0 && isToday) Color.White
                        else if (v > 0) Color.White.copy(alpha = 0.55f)
                        else Color.White.copy(alpha = 0.22f)
                    )
            )
        }
    }
}

@Composable
private fun SoftTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun CoreCard(total: Int, familiar: Int, met: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Core ${total}개", style = MaterialTheme.typography.titleMedium)
                Text(
                    "익숙 $familiar · 만남 $met",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SoftBar(if (total == 0) 0f else familiar.toFloat() / total, masteryColors[3])
            Text(
                "get, take, make 같은 기본 어휘를 뜻·용법·연어·구동사까지 정복하는 트랙",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LevelRow(
    name: String,
    isCurrent: Boolean,
    familiar: Int,
    total: Int,
    last: Boolean,
    onClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Surface(
            onClick = onClick,
            color = Color.Transparent,
            shape = MaterialTheme.shapes.extraSmall,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(name, style = MaterialTheme.typography.titleSmall)
                        if (isCurrent) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    "학습 중",
                                    Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                    VSpace(7)
                    SoftBar(if (total == 0) 0f else familiar.toFloat() / total, MaterialTheme.colorScheme.primary, height = 5)
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    "$familiar/$total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!last) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

/** 카드 안에서 쓰는 얇은 진행 막대. */
@Composable
private fun SoftBar(progress: Float, color: Color, height: Int = 7) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 200f),
        label = "softbar",
    )
    Box(
        Modifier
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
                    .background(color)
            )
        }
    }
}
