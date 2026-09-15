package dev.brown.wordy.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.brown.wordy.ui.components.LoadingBox
import dev.brown.wordy.ui.components.ProgressBar
import dev.brown.wordy.ui.components.SectionTitle
import dev.brown.wordy.ui.components.StatTile
import dev.brown.wordy.ui.components.VSpace
import dev.brown.wordy.ui.components.appViewModel
import dev.brown.wordy.ui.theme.MasteryColors

@Composable
fun HomeScreen(onStartSession: (extraNew: Int) -> Unit, onOpenCore: () -> Unit, onOpenLevel: (dev.brown.wordy.domain.model.Level) -> Unit) {
    val vm = appViewModel { HomeViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    if (s.loading) { LoadingBox(); return }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("오늘의 학습", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            if (s.dueNow == 0 && s.learnedToday >= s.dailyGoal) "오늘 목표를 다 채웠어요. 더 배우기로 이어갈 수 있어요."
            else "복습 ${s.dueNow}개 · 새 단어 ${(s.dailyGoal - s.learnedToday).coerceAtLeast(0)}개가 기다리고 있어요.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VSpace(14)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("복습 대기", "${s.dueNow}", Modifier.weight(1f))
            StatTile("오늘 새 단어", "${s.learnedToday}/${s.dailyGoal}", Modifier.weight(1f))
            StatTile("연속 학습", "${s.streak}일", Modifier.weight(1f))
        }
        VSpace(14)
        val remainingNew = (s.dailyGoal - s.learnedToday).coerceAtLeast(0)
        val nothingLeft = s.dueNow == 0 && remainingNew == 0
        Button(onClick = { onStartSession(if (nothingLeft) 5 else 0) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(if (nothingLeft) "5개 더 배우기" else "학습 시작", style = MaterialTheme.typography.titleMedium)
        }
        if (s.dueTomorrow > 0) {
            VSpace(6)
            Text("내일 예정 복습 ${s.dueTomorrow}개", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        SectionTitle("핵심 회화 어휘")
        Card(onClick = onOpenCore, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Core ${s.coreTotal}개", fontWeight = FontWeight.SemiBold)
                    Text("익숙 ${s.coreFamiliar} · 만남 ${s.coreMet}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ProgressBar(if (s.coreTotal == 0) 0f else s.coreFamiliar.toFloat() / s.coreTotal, color = MasteryColors[3])
                Text("get, take, make 같은 기본 동사·명사·형용사를 뜻·용법·연어·구동사까지 정복하는 트랙", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        SectionTitle("레벨별 진행")
        s.levels.forEach { lp ->
            Card(onClick = { onOpenLevel(lp.level) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(lp.level.ko, fontWeight = FontWeight.SemiBold)
                            if (lp.level == s.currentLevel) Text("현재", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("${lp.familiar}/${lp.total}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ProgressBar(if (lp.total == 0) 0f else lp.familiar.toFloat() / lp.total)
                }
            }
        }
        VSpace(12)
        OutlinedButton(onClick = { onStartSession(5) }, modifier = Modifier.fillMaxWidth()) { Text("오늘 목표와 별개로 5개 더 배우기") }
        VSpace(8)
        Text("콘텐츠 ${s.contentVersion} · 단어 ${s.totalWords}개 · 누적 복습 ${s.totalReviews}회", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.align(Alignment.CenterHorizontally))
        VSpace(24)
    }
}
