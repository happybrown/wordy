package dev.brown.wordy.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.brown.wordy.domain.model.Mastery
import dev.brown.wordy.ui.components.LoadingBox
import dev.brown.wordy.ui.components.MasteryDot
import dev.brown.wordy.ui.components.SectionTitle
import dev.brown.wordy.ui.components.StatTile
import dev.brown.wordy.ui.components.VSpace
import dev.brown.wordy.ui.components.appViewModel
import dev.brown.wordy.ui.theme.masteryColors
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(onWord: (String) -> Unit) {
    val vm = appViewModel { c -> StatsViewModel(c) }
    val s by vm.state.collectAsStateWithLifecycle()

    if (s.loading) { LoadingBox(); return }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("통계", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        SectionTitle("최근 30일")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("복습", "${s.reviews30d}", Modifier.weight(1f))
            StatTile(
                "정답률",
                s.recentAccuracy?.let { "${(it * 100).toInt()}%" } ?: "—",
                Modifier.weight(1f),
                accent = s.recentAccuracy?.takeIf { it >= 0.8 }?.let { MaterialTheme.colorScheme.primary },
            )
            StatTile("학습일", "${s.activeDays30d}일", Modifier.weight(1f))
        }
        VSpace(8)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("연속", "${s.streak}일", Modifier.weight(1f))
            StatTile("학습 시간", formatDuration(s.seconds30d), Modifier.weight(1f))
            StatTile("누적 복습", "${s.totalReviews}", Modifier.weight(1f))
        }

        VSpace(16)
        DayChart(s.days)

        SectionTitle("숙달 단계")
        MasteryBreakdown(s.mastery, s.introduced)
        s.avgDaysToMaster?.let {
            VSpace(6)
            Text("숙달까지 평균 ${"%.1f".format(it)}일", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SectionTitle("약한 단어")
        if (s.weakWords.isEmpty()) {
            Text("아직 틀린 단어가 없습니다. 복습을 더 하면 여기에 모입니다.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("잊은 횟수가 많고 기억이 짧게 유지되는 순서입니다.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            VSpace(8)
            s.weakWords.forEach { w ->
                Row(
                    Modifier.fillMaxWidth().clickable { onWord(w.wordId) }.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(w.headword, fontWeight = FontWeight.SemiBold)
                        if (w.ko.isNotBlank()) {
                            Text(w.ko, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        "${w.lapses}번 잊음 · ${formatStability(w.stabilityDays)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
            }
        }
        VSpace(32)
    }
}

/** 학습 시간. 1분 미만을 0분으로 보여 주면 방금 한 세션이 사라진 것처럼 보인다. */
private fun formatDuration(seconds: Int): String = when {
    seconds == 0 -> "0분"
    seconds < 60 -> "${seconds}초"
    seconds < 3600 -> "${seconds / 60}분"
    else -> "${seconds / 3600}시간 ${(seconds % 3600) / 60}분"
}

/** 기억 유지 기간. 하루 미만은 시간으로 보여 준다. */
private fun formatStability(days: Double): String = when {
    days < 1.0 -> "${(days * 24).toInt().coerceAtLeast(1)}시간 유지"
    days < 30.0 -> "${days.toInt()}일 유지"
    else -> "${"%.1f".format(days / 30.0)}개월 유지"
}

/** 최근 30일 복습량 막대. 값이 아니라 추세를 보는 것이 목적이라 축 숫자는 넣지 않는다. */
@Composable
private fun DayChart(days: List<DayBar>) {
    val max = days.maxOfOrNull { it.reviewed + it.newLearned } ?: 0
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(14.dp)) {
            if (max == 0) {
                Text("아직 학습 기록이 없습니다.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(Modifier.fillMaxWidth().height(104.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    days.forEachIndexed { i, d ->
                        val total = d.reviewed + d.newLearned
                        val frac = if (max == 0) 0f else total.toFloat() / max
                        val isToday = i == days.lastIndex
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.fillMaxWidth()
                                    .height((frac * 92).dp.coerceAtLeast(if (total > 0) 6.dp else 3.dp))
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            total > 0 && isToday -> MaterialTheme.colorScheme.primary
                                            total > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        }
                                    )
                            )
                        }
                    }
                }
                VSpace(6)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val fmt = DateTimeFormatter.ofPattern("M/d")
                    Text(days.first().date.format(fmt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("오늘", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MasteryBreakdown(counts: Map<Mastery, Int>, introduced: Int) {
    // 아직 안 배운 단어(NEW)는 빼고 본다. 4,054개 중 대부분이라 그래프를 압도한다.
    val shown = Mastery.entries.filter { it != Mastery.NEW }
    val total = shown.sumOf { counts[it] ?: 0 }

    if (total == 0) {
        Text("아직 배운 단어가 없습니다.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    Row(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        shown.forEach { m ->
            val n = counts[m] ?: 0
            if (n > 0) Box(Modifier.weight(n.toFloat()).fillMaxWidth().height(14.dp).clip(CircleShape).background(masteryColors[m.level]))
        }
    }
    VSpace(10)
    shown.forEach { m ->
        val n = counts[m] ?: 0
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MasteryDot(m, Modifier.size(10.dp))
            Text(m.ko, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("$n", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
    VSpace(4)
    Text("배우기 시작한 단어 ${introduced}개", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
