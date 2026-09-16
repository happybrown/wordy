package dev.brown.wordy.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.brown.wordy.ui.components.ProgressBar
import dev.brown.wordy.ui.components.SectionTitle
import dev.brown.wordy.ui.components.VSpace
import dev.brown.wordy.ui.components.appViewModel
import dev.brown.wordy.ui.theme.masteryColors

@Composable
fun HomeScreen(onStartSession: (extraNew: Int) -> Unit, onOpenCore: () -> Unit, onOpenLevel: (Level) -> Unit) {
    val vm = appViewModel { HomeViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    if (s.loading) { LoadingBox(); return }

    val remainingNew = (s.dailyGoal - s.learnedToday).coerceAtLeast(0)
    val nothingLeft = s.dueNow == 0 && remainingNew == 0

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        VSpace(16)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("오늘의 학습", style = MaterialTheme.typography.headlineMedium)
            if (s.streak > 0) StreakBadge(s.streak)
        }

        VSpace(16)
        TodayCard(
            dueNow = s.dueNow,
            learnedToday = s.learnedToday,
            dailyGoal = s.dailyGoal,
            dueTomorrow = s.dueTomorrow,
            nothingLeft = nothingLeft,
            onStart = { onStartSession(if (nothingLeft) 5 else 0) },
        )

        SectionTitle("핵심 회화 어휘")
        TrackCard(
            title = "Core ${s.coreTotal}개",
            trailing = "익숙 ${s.coreFamiliar}",
            progress = if (s.coreTotal == 0) 0f else s.coreFamiliar.toFloat() / s.coreTotal,
            color = masteryColors[3],
            desc = "get, take, make 같은 기본 어휘를 뜻·용법·연어·구동사까지 정복하는 트랙",
            onClick = onOpenCore,
        )

        SectionTitle("레벨별 진행")
        s.levels.forEachIndexed { i, lp ->
            if (i > 0) VSpace(8)
            LevelRow(
                name = lp.level.ko,
                isCurrent = lp.level == s.currentLevel,
                familiar = lp.familiar,
                total = lp.total,
                onClick = { onOpenLevel(lp.level) },
            )
        }

        VSpace(20)
        if (!nothingLeft) {
            TextButton(onClick = { onStartSession(5) }, modifier = Modifier.fillMaxWidth()) {
                Text("오늘 목표와 별개로 5개 더 배우기")
            }
        }
        VSpace(4)
        Text(
            "단어 ${s.totalWords}개 · 누적 복습 ${s.totalReviews}회\n콘텐츠 ${s.contentVersion}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        VSpace(28)
    }
}

/**
 * 오늘 할 일 카드. 화면에서 가장 중요한 한 가지(=지금 시작하기)를 크게 감싼다.
 * 숫자 타일 세 개를 나란히 놓던 것을 하나로 합쳤다. 타일이 여러 개면 시선이
 * 분산돼 정작 눌러야 할 버튼이 묻힌다.
 */
@Composable
private fun TodayCard(
    dueNow: Int,
    learnedToday: Int,
    dailyGoal: Int,
    dueTomorrow: Int,
    nothingLeft: Boolean,
    onStart: () -> Unit,
) {
    val goalProgress = if (dailyGoal == 0) 1f else (learnedToday.toFloat() / dailyGoal).coerceIn(0f, 1f)
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            if (nothingLeft) {
                Text("오늘 할 몫을 다 했어요", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                VSpace(4)
                Text(
                    "더 하고 싶으면 5개를 추가로 배울 수 있어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    BigNumber(dueNow, "복습")
                    VSpaceWidth()
                    BigNumber((dailyGoal - learnedToday).coerceAtLeast(0), "새 단어")
                }
                VSpace(14)
                ProgressBar(
                    goalProgress,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    height = 8,
                )
                VSpace(6)
                Text(
                    "오늘 목표 $learnedToday/$dailyGoal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }

            VSpace(18)
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = CircleShape,
            ) {
                Text(if (nothingLeft) "5개 더 배우기" else "학습 시작", style = MaterialTheme.typography.titleMedium)
                VSpaceWidth(8)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
            }
            if (dueTomorrow > 0) {
                VSpace(10)
                Text(
                    "내일 예정 복습 ${dueTomorrow}개",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BigNumber(value: Int, label: String) {
    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 200f),
        label = "bigNumber",
    )
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            "${animated.toInt()}",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            " $label",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            modifier = Modifier.padding(bottom = 5.dp),
        )
    }
}

/** 연속 학습 배지. 숫자만 있으면 밋밋해서 불꽃 아이콘을 붙였다. */
@Composable
private fun StreakBadge(days: Int) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Text(
                "${days}일 연속",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun TrackCard(
    title: String,
    trailing: String,
    progress: Float,
    color: Color,
    desc: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(trailing, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ProgressBar(progress, color = color)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * 레벨 한 줄. 카드를 쌓는 대신 얇은 행으로 바꿔 다섯 레벨이 한눈에 들어오게 했다.
 * 진행률 숫자는 오른쪽 끝에 고정해 세로로 읽힌다.
 */
@Composable
private fun LevelRow(name: String, isCurrent: Boolean, familiar: Int, total: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name, style = MaterialTheme.typography.titleSmall)
                    if (isCurrent) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Text("학습 중", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text("$familiar/$total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ProgressBar(if (total == 0) 0f else familiar.toFloat() / total, height = 6)
        }
    }
}

/** 가로 간격. Row 안에서 Spacer 를 매번 만들지 않으려고 둔다. */
@Composable
private fun VSpaceWidth(dp: Int = 20) = androidx.compose.foundation.layout.Spacer(Modifier.size(dp.dp))
