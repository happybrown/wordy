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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.brown.wordy.ui.theme.heroGradient
import dev.brown.wordy.ui.theme.onHeroGradient
import dev.brown.wordy.ui.theme.tileBackgrounds
import dev.brown.wordy.ui.theme.tileForegrounds

@Composable
fun HomeScreen(
    onStartSession: (extraNew: Int) -> Unit,
    onStartQuiz: (count: Int) -> Unit,
    onOpenCore: () -> Unit,
    onOpenLevel: (Level) -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    if (s.loading) { LoadingBox(); return }

    val remainingNew = (s.dailyGoal - s.learnedToday).coerceAtLeast(0)
    val waiting = s.dueNow + remainingNew
    val nothingLeft = waiting == 0
    var showQuizPicker by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        VSpace(18)

        // 인사말. 참고 이미지처럼 작은 줄 위에 큰 이름을 둔다.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    "오늘의 학습",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VSpace(2)
                Text(
                    if (nothingLeft) "다 끝냈어요" else "${waiting}개가 기다려요",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            if (s.streak > 0) StreakPill(s.streak)
        }

        VSpace(18)
        HeroCard(
            dueNow = s.dueNow,
            remainingNew = remainingNew,
            learnedToday = s.learnedToday,
            dailyGoal = s.dailyGoal,
            week = s.weekActivity,
            nothingLeft = nothingLeft,
            onStart = { onStartSession(if (nothingLeft) 5 else 0) },
        )

        VSpace(12)
        QuizCard(enabled = s.familiarTotal > 0 || s.introducedTotal > 0, onClick = { showQuizPicker = true })

        // 레벨. 참고 이미지의 과목 타일 격자.
        VSpace(28)
        SectionHead("레벨", "쉬운 단계부터 차례로")
        VSpace(14)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            s.levels.forEachIndexed { i, lp ->
                LevelTile(
                    name = lp.level.shortKo,
                    familiar = lp.familiar,
                    total = lp.total,
                    isCurrent = lp.level == s.currentLevel,
                    index = i,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenLevel(lp.level) },
                )
            }
        }

        // 상세 목록. 참고 이미지의 "Subject Details" 처럼 아이콘 + 제목 + 설명 두 줄.
        VSpace(28)
        SectionHead("학습 현황", null)
        VSpace(14)
        Surface(
            Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                DetailRow(
                    tile = 5,
                    icon = Icons.Outlined.ChatBubbleOutline,
                    title = "Core ${s.coreTotal}개",
                    desc = "get, take, make 같은 기본 어휘를 용법까지",
                    trailing = "익숙 ${s.coreFamiliar}",
                    onClick = onOpenCore,
                )
                RowDivider()
                DetailRow(
                    tile = 3,
                    icon = Icons.Outlined.CheckCircle,
                    title = "익힌 단어 ${s.familiarTotal}개",
                    desc = "'익숙' 단계 이상에 오른 단어",
                    trailing = null,
                    onClick = null,
                )
                RowDivider()
                DetailRow(
                    tile = 2,
                    icon = Icons.Outlined.Schedule,
                    title = "내일 복습 ${s.dueTomorrow}개",
                    desc = "알고리즘이 잡아 준 내일 분량",
                    trailing = null,
                    onClick = null,
                )
            }
        }

        VSpace(22)
        Text(
            "단어 ${s.totalWords}개 · 누적 복습 ${s.totalReviews}회",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        VSpace(26)
    }

    if (showQuizPicker) {
        QuizCountSheet(
            onDismiss = { showQuizPicker = false },
            onPick = { n -> showQuizPicker = false; onStartQuiz(n) },
        )
    }
}

/**
 * 퀴즈 전용 진입점. 새 단어를 배우지 않고 이미 아는 단어만 문제로 푼다.
 * 배운 단어가 하나도 없으면 낼 문제가 없으므로 눌리지 않게 한다.
 */
@Composable
private fun QuizCard(enabled: Boolean, onClick: () -> Unit) {
    val bg = tileBackgrounds[2]
    val fg = tileForegrounds[2]
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(MaterialTheme.shapes.extraSmall).background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Quiz, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("퀴즈만 풀기", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (enabled) "새 단어 없이 아는 단어만 문제로" else "먼저 단어를 몇 개 배워야 해요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

/** 문제 수 선택. 한 손으로 고를 수 있게 네 개만 둔다. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun QuizCountSheet(onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("몇 문제 풀까요?", style = MaterialTheme.typography.titleLarge)
            VSpace(4)
            Text(
                "복습할 때가 된 단어를 먼저 내고, 모자라면 곧 복습할 단어를 당겨 옵니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VSpace(18)
            listOf(10, 20, 30, 50).forEach { n ->
                Surface(
                    onClick = { onPick(n) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${n}문제", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(
                            "약 ${n / 5}분",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHead(title: String, desc: String?) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (desc != null) {
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * 히어로 카드. 화면에서 색을 쓰는 유일한 큰 면이다.
 * 오른쪽 위 반투명 원은 평평한 그라데이션에 깊이를 준다.
 */
@Composable
private fun HeroCard(
    dueNow: Int,
    remainingNew: Int,
    learnedToday: Int,
    dailyGoal: Int,
    week: List<Int>,
    nothingLeft: Boolean,
    onStart: () -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = Color.Transparent) {
        Box(Modifier.background(heroGradient)) {
            Box(
                Modifier
                    .size(140.dp)
                    .offset(x = 252.dp, y = (-64).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.11f))
            )
            Column(Modifier.padding(20.dp)) {
                if (nothingLeft) {
                    Text("오늘 할 몫을 다 했어요", style = MaterialTheme.typography.titleLarge, color = onHeroGradient)
                    VSpace(4)
                    Text(
                        "더 하고 싶으면 5개를 추가로 배울 수 있어요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onHeroGradient.copy(alpha = 0.8f),
                    )
                } else {
                    // 복습과 새 단어를 나눠 보여 준다. 합계만 보면 무엇이 밀렸는지 알 수 없다.
                    Row {
                        HeroStat(dueNow, "복습")
                        Spacer(Modifier.width(26.dp))
                        HeroStat(remainingNew, "새 단어")
                    }
                    VSpace(16)
                    WeekBars(week)
                    VSpace(8)
                    Text(
                        "오늘 목표 $learnedToday/$dailyGoal",
                        style = MaterialTheme.typography.labelMedium,
                        color = onHeroGradient.copy(alpha = 0.72f),
                    )
                }

                VSpace(18)
                Surface(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = CircleShape,
                    color = Color.White,
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (nothingLeft) "5개 더 배우기" else "학습 시작",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(7.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            null,
                            Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStat(value: Int, label: String) {
    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 180f),
        label = "heroStat",
    )
    Column {
        Text("${animated.toInt()}", style = MaterialTheme.typography.displaySmall, color = onHeroGradient)
        Text(label, style = MaterialTheme.typography.labelMedium, color = onHeroGradient.copy(alpha = 0.72f))
    }
}

/** 최근 7일 학습 리듬. 값이 아니라 흐름을 보는 것이라 축이나 숫자를 넣지 않는다. */
@Composable
private fun WeekBars(week: List<Int>) {
    val max = (week.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        week.forEachIndexed { i, v ->
            val isToday = i == week.lastIndex
            Box(
                Modifier
                    .weight(1f)
                    // 학습이 없는 날도 흔적은 남겨 7일 리듬이 보이게 한다
                    .fillMaxHeight(if (v > 0) (v.toFloat() / max).coerceAtLeast(0.3f) else 0.16f)
                    .clip(CircleShape)
                    .background(
                        when {
                            v > 0 && isToday -> Color.White
                            v > 0 -> Color.White.copy(alpha = 0.72f)
                            else -> Color.White.copy(alpha = 0.3f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun StreakPill(days: Int) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(15.dp),
            )
            Text(
                "$days",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

/**
 * 레벨 타일. 파스텔 배경에 난이도 계단 막대.
 * 타일 안에 레벨 이름을 넣으면 바로 아래 라벨과 같은 글자가 두 번 나온다.
 */
@Composable
private fun LevelTile(
    name: String,
    familiar: Int,
    total: Int,
    isCurrent: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg = tileBackgrounds[index % tileBackgrounds.size]
    val fg = tileForegrounds[index % tileForegrounds.size]
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(62.dp),
            shape = MaterialTheme.shapes.small,
            color = bg,
        ) {
            Column(
                Modifier.padding(6.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DifficultyGlyph(step = index, color = fg)
                VSpace(6)
                Box(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(fg.copy(alpha = 0.2f))
                ) {
                    val p = if (total == 0) 0f else familiar.toFloat() / total
                    if (p > 0f) Box(Modifier.fillMaxWidth(p).fillMaxHeight().clip(CircleShape).background(fg))
                }
            }
        }
        VSpace(7)
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isCurrent) fg else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** 난이도 글리프. 막대 5개 중 [step]+1 개를 채워 계단을 만든다. */
@Composable
private fun DifficultyGlyph(step: Int, color: Color) {
    Row(
        Modifier.height(19.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(5) { i ->
            Box(
                Modifier
                    .width(3.5.dp)
                    .fillMaxHeight(0.32f + 0.17f * i)
                    .clip(CircleShape)
                    .background(if (i <= step) color else color.copy(alpha = 0.22f))
            )
        }
    }
}

/**
 * 상세 한 줄. 참고 이미지의 "18 online lectures / Lectures can be viewed…" 구조.
 * 왼쪽에 파스텔 아이콘 타일, 가운데 제목과 설명, 오른쪽에 수치.
 */
@Composable
private fun DetailRow(
    tile: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    trailing: String?,
    onClick: (() -> Unit)?,
) {
    val bg = tileBackgrounds[tile % tileBackgrounds.size]
    val fg = tileForegrounds[tile % tileForegrounds.size]
    val content: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(MaterialTheme.shapes.extraSmall).background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(10.dp))
                Text(
                    trailing,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, color = Color.Transparent) { content() }
    } else {
        content()
    }
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .padding(start = 69.dp, end = 16.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
