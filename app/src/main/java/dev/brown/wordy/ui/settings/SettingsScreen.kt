package dev.brown.wordy.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.brown.wordy.data.prefs.Settings
import dev.brown.wordy.domain.model.Level
import dev.brown.wordy.ui.components.LocalAppContainer
import dev.brown.wordy.ui.components.SectionTitle
import dev.brown.wordy.ui.components.VSpace
import kotlinx.coroutines.launch
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import dev.brown.wordy.platform.ReminderScheduler

@Composable
fun SettingsScreen() {
    val c = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val s by c.settings.settings.collectAsStateWithLifecycle(initialValue = Settings())
    fun update(block: (Settings) -> Settings) = scope.launch { c.settings.update(block) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("설정", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        SectionTitle("학습")
        Text("현재 레벨", fontWeight = FontWeight.SemiBold)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Level.entries.forEach { lvl -> FilterChip(selected = s.currentLevel == lvl, onClick = { update { it.copy(currentLevel = lvl) } }, label = { Text(lvl.ko) }) }
        }
        VSpace(12)
        Text("하루 새 단어 ${s.dailyNewWords}개", fontWeight = FontWeight.SemiBold)
        Slider(value = s.dailyNewWords.toFloat(), onValueChange = { update { st -> st.copy(dailyNewWords = it.toInt()) } }, valueRange = 0f..40f, steps = 39)
        ToggleRow("핵심 회화 어휘 트랙", "새 단어의 일부를 Core 트랙에서 뽑습니다", s.coreEnabled) { update { st -> st.copy(coreEnabled = it) } }
        if (s.coreEnabled) {
            Text("Core 비율 ${(s.coreRatio * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(value = s.coreRatio, onValueChange = { update { st -> st.copy(coreRatio = it) } }, valueRange = 0.1f..0.9f, steps = 7)
        }
        Text("목표 기억 보존률 ${(s.desiredRetention * 100).toInt()}%", fontWeight = FontWeight.SemiBold)
        Text("높이면 복습이 잦아지고, 낮추면 복습은 줄지만 더 자주 잊습니다. 기본 90%.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = s.desiredRetention, onValueChange = { update { st -> st.copy(desiredRetention = it) } }, valueRange = 0.8f..0.95f, steps = 14)
        ToggleRow("엄격한 타이핑 판정", "켜면 1글자 오타도 오답으로 처리합니다", s.strictTyping) { update { st -> st.copy(strictTyping = it) } }

        SectionTitle("알림")
        val ctx = LocalContext.current
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) { update { st -> st.copy(reminderEnabled = true) }; ReminderScheduler.schedule(ctx, s.reminderHour, s.reminderMinute) }
        }
        ToggleRow("매일 복습 알림", "설정한 시각에 예정된 복습 수를 알려 줍니다", s.reminderEnabled) { on ->
            if (!on) { update { st -> st.copy(reminderEnabled = false) }; ReminderScheduler.cancel(ctx) }
            else if (Build.VERSION.SDK_INT >= 33 && !ReminderScheduler.hasPermission(ctx)) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else { update { st -> st.copy(reminderEnabled = true) }; ReminderScheduler.schedule(ctx, s.reminderHour, s.reminderMinute) }
        }
        if (s.reminderEnabled) {
            Text("알림 시각 ${"%02d:%02d".format(s.reminderHour, s.reminderMinute)}", fontWeight = FontWeight.SemiBold)
            Slider(value = (s.reminderHour * 60 + s.reminderMinute).toFloat(), onValueChange = { v -> val m = v.toInt(); update { st -> st.copy(reminderHour = m / 60, reminderMinute = (m % 60) / 30 * 30) } },
                onValueChangeFinished = { ReminderScheduler.schedule(ctx, s.reminderHour, s.reminderMinute) }, valueRange = 0f..1439f, steps = 47)
        }

        SectionTitle("발음")
        Text("TTS 속도 ${"%.2f".format(s.ttsRate)}x", fontWeight = FontWeight.SemiBold)
        Slider(value = s.ttsRate, onValueChange = { update { st -> st.copy(ttsRate = it) }; c.tts.rate = it }, valueRange = 0.6f..1.3f, steps = 13)

        SectionTitle("정보")
        Text("콘텐츠 버전 ${c.contentVersion}", style = MaterialTheme.typography.bodyMedium)
        VSpace(6)
        Text(
            "단어 목록 출처: New General Service List, New Academic Word List, Business Service List (Browne, Culligan & Phillips; CC BY-SA 4.0) · " +
                "The CEFR-J Wordlist Version 1.6 (Compiled by Yukio Tono, Tokyo University of Foreign Studies) · " +
                "교육부 고시 제2015-74호 영어과 교육과정 기본 어휘 목록. 뜻·예문·설명은 Wordy 자체 제작. 간격 반복은 FSRS-6 알고리즘(open-spaced-repetition).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VSpace(32)
    }
}

@Composable
private fun ToggleRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
    HorizontalDivider()
}
