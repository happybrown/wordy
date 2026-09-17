package dev.brown.wordy.ui.onboarding

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.brown.wordy.domain.model.Level
import dev.brown.wordy.ui.components.LocalAppContainer
import dev.brown.wordy.ui.components.VSpace
import kotlinx.coroutines.launch

private val LEVEL_DESC = mapOf(
    Level.PRE to "cat, red, jump — 초등 교육과정에 들어가기 전, 가장 기본적인 사물과 동작",
    Level.ELEM to "breakfast, remember, quiet — 초등 교육과정 800단어 수준",
    Level.MID to "environment, suggest, disappointed — 중학 교육과정, CEFR A2~B1",
    Level.HIGH to "significant, accomplish, inevitable — 고교 교육과정, CEFR B1~B2",
    Level.ADV to "ambiguous, leverage, scrutinize — 고교 과정 밖의 학술·비즈니스 어휘",
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val c = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var level by remember { mutableStateOf(Level.ELEM) }
    var goal by remember { mutableFloatStateOf(10f) }
    var core by remember { mutableStateOf(true) }
    var showPlacement by remember { mutableStateOf(false) }
    var placedBy by remember { mutableStateOf<String?>(null) }

    if (showPlacement) {
        PlacementScreen(onDone = { recommended ->
            if (recommended != null) { level = recommended; placedBy = "진단 결과" }
            showPlacement = false
        })
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        VSpace(24)
        Text("Wordy", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("뜻만 외우는 단어장이 아니라, 회화에서 바로 쓸 수 있게 익히는 앱입니다.", style = MaterialTheme.typography.bodyLarge)
        VSpace(24)
        Text("시작 레벨", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("언제든 설정에서 바꿀 수 있어요. 낮은 레벨부터 빠르게 훑는 것도 좋습니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        VSpace(8)
        OutlinedButton(
            onClick = { showPlacement = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
        ) {
            Text(if (placedBy == null) "30문제로 내 레벨 진단하기" else "다시 진단하기")
        }
        placedBy?.let {
            VSpace(4)
            Text("$it: ${level.ko}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        VSpace(8)
        Level.entries.forEach { lvl ->
            val selected = lvl == level
            Card(
                onClick = { level = lvl; placedBy = null }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(lvl.ko, fontWeight = FontWeight.SemiBold)
                    Text(LEVEL_DESC[lvl] ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        VSpace(20)
        Text("하루 새 단어 ${goal.toInt()}개", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("복습은 알고리즘이 자동으로 잡아 줍니다. 처음엔 10개 전후를 권합니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = goal, onValueChange = { goal = it }, valueRange = 3f..30f, steps = 26)
        VSpace(12)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("핵심 회화 어휘 트랙 함께 진행", fontWeight = FontWeight.SemiBold)
                Text("get, take, make 같은 기본 어휘를 의미별로 깊게. 새 단어의 40%를 여기서 뽑습니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = core, onCheckedChange = { core = it })
        }
        VSpace(28)
        Button(
            onClick = { scope.launch { c.settings.update { it.copy(onboardingDone = true, currentLevel = level, dailyNewWords = goal.toInt(), coreEnabled = core) }; onDone() } },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
        ) { Text("시작하기", style = MaterialTheme.typography.titleMedium) }
        VSpace(24)
    }
}
