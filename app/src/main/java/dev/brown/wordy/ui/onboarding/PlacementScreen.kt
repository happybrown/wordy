package dev.brown.wordy.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.brown.wordy.domain.model.Level
import dev.brown.wordy.ui.components.LoadingBox
import dev.brown.wordy.ui.components.VSpace
import dev.brown.wordy.ui.components.appViewModel

/**
 * 레벨 진단 퀴즈. 결과는 추천일 뿐이고 [onDone] 을 받은 쪽에서 그대로 쓸지 정한다.
 * 중간에 그만둘 수 있어야 한다 — 30문항을 강제하면 온보딩에서 이탈한다.
 */
@Composable
fun PlacementScreen(onDone: (Level?) -> Unit) {
    val vm = appViewModel { c -> PlacementViewModel(c) }
    val s by vm.state.collectAsStateWithLifecycle()

    if (s.loading) { LoadingBox(); return }

    if (s.finished) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("진단 완료", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            VSpace(12)
            Text(s.summary, style = MaterialTheme.typography.bodyLarge)
            VSpace(8)
            Text(
                "추천 레벨보다 낮게 시작해 빠르게 훑는 것도 좋습니다. 설정에서 언제든 바꿀 수 있어요.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VSpace(24)
            Button(onClick = { onDone(s.recommended) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("${s.recommended.ko} 레벨로 시작하기")
            }
            VSpace(8)
            TextButton(onClick = { onDone(null) }, modifier = Modifier.fillMaxWidth()) { Text("직접 고를게요") }
        }
        return
    }

    val q = s.current ?: return
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        VSpace(12)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("레벨 진단", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${s.index + 1} / ${s.total}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        VSpace(8)
        LinearProgressIndicator(progress = { (s.index).toFloat() / s.total.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())

        VSpace(32)
        Text(q.headword, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(q.pos, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        VSpace(28)

        q.options.forEachIndexed { i, opt ->
            OutlinedButton(
                onClick = { vm.answer(i) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).height(56.dp),
            ) { Text(opt, style = MaterialTheme.typography.bodyLarge) }
        }

        VSpace(16)
        // 모르는 단어를 찍게 하면 진단이 부정확해진다. 모른다고 답하는 길을 준다.
        TextButton(onClick = { vm.answer(-1) }, modifier = Modifier.fillMaxWidth()) { Text("모르겠어요") }
        TextButton(onClick = { onDone(null) }, modifier = Modifier.fillMaxWidth()) { Text("진단 건너뛰기") }
    }
}
