package dev.brown.wordy.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.brown.wordy.data.prefs.Settings
import dev.brown.wordy.domain.model.Level
import dev.brown.wordy.ui.components.LocalAppContainer
import dev.brown.wordy.ui.components.SectionTitle
import dev.brown.wordy.ui.theme.tileBackgrounds
import dev.brown.wordy.ui.theme.tileForegrounds
import dev.brown.wordy.ui.components.VSpace
import kotlinx.coroutines.launch
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import dev.brown.wordy.platform.BackupManager
import dev.brown.wordy.platform.ReminderScheduler

@Composable
fun SettingsScreen() {
    val c = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val s by c.settings.settings.collectAsStateWithLifecycle(initialValue = Settings())
    fun update(block: (Settings) -> Settings) = scope.launch { c.settings.update(block) }
    val ctx = LocalContext.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        VSpace(18)
        Text("설정", style = MaterialTheme.typography.headlineMedium)
        Text(
            "학습량과 알림을 조절합니다",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // --- 학습 ---
        GroupHeader(0, Icons.Outlined.School, "학습")
        SettingsGroup {
            Text("현재 레벨", style = MaterialTheme.typography.titleSmall)
            VSpace(2)
            Text(
                "새 단어를 어느 레벨에서 뽑을지 정합니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VSpace(10)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Level.entries.forEach { lvl ->
                    FilterChip(
                        selected = s.currentLevel == lvl,
                        onClick = { update { it.copy(currentLevel = lvl) } },
                        label = { Text(lvl.ko) },
                        shape = CircleShape,
                    )
                }
            }

            RowGap()
            SliderRow(
                title = "하루 새 단어",
                value = "${s.dailyNewWords}개",
                desc = "0으로 두면 복습만 합니다",
                slider = {
                    Slider(
                        value = s.dailyNewWords.toFloat(),
                        onValueChange = { update { st -> st.copy(dailyNewWords = it.toInt()) } },
                        valueRange = 0f..40f, steps = 39,
                    )
                },
            )

            RowGap()
            ToggleRow(
                "핵심 회화 어휘 트랙",
                "새 단어의 일부를 Core 트랙에서 뽑습니다",
                s.coreEnabled,
                divider = false,
            ) { update { st -> st.copy(coreEnabled = it) } }
            if (s.coreEnabled) {
                // 토글에 딸린 하위 설정이라 들여써서 종속 관계를 보이게 한다
                Column(Modifier.padding(start = 14.dp, top = 10.dp)) {
                    SliderRow(
                        title = "Core 비율",
                        value = "${(s.coreRatio * 100).toInt()}%",
                        desc = null,
                        slider = {
                            Slider(
                                value = s.coreRatio,
                                onValueChange = { update { st -> st.copy(coreRatio = it) } },
                                valueRange = 0.1f..0.9f, steps = 7,
                            )
                        },
                    )
                }
            }

            RowGap()
            SliderRow(
                title = "목표 기억 보존률",
                value = "${(s.desiredRetention * 100).toInt()}%",
                desc = "높이면 복습이 잦아지고, 낮추면 복습은 줄지만 더 자주 잊습니다",
                slider = {
                    Slider(
                        value = s.desiredRetention,
                        onValueChange = { update { st -> st.copy(desiredRetention = it) } },
                        valueRange = 0.8f..0.95f, steps = 14,
                    )
                },
            )

            RowGap()
            ToggleRow(
                "엄격한 타이핑 판정",
                "켜면 1글자 오타도 오답으로 처리합니다",
                s.strictTyping,
                divider = false,
            ) { update { st -> st.copy(strictTyping = it) } }
        }

        // --- 알림 ---
        GroupHeader(1, Icons.Outlined.Notifications, "알림")
        SettingsGroup {
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    update { st -> st.copy(reminderEnabled = true) }
                    ReminderScheduler.schedule(ctx, s.reminderHour, s.reminderMinute)
                }
            }
            ToggleRow(
                "매일 복습 알림",
                "설정한 시각에 예정된 복습 수를 알려 줍니다",
                s.reminderEnabled,
                divider = false,
            ) { on ->
                if (!on) {
                    update { st -> st.copy(reminderEnabled = false) }; ReminderScheduler.cancel(ctx)
                } else if (Build.VERSION.SDK_INT >= 33 && !ReminderScheduler.hasPermission(ctx)) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    update { st -> st.copy(reminderEnabled = true) }
                    ReminderScheduler.schedule(ctx, s.reminderHour, s.reminderMinute)
                }
            }
            if (s.reminderEnabled) {
                Column(Modifier.padding(start = 14.dp, top = 10.dp)) {
                SliderRow(
                    title = "알림 시각",
                    value = "%02d:%02d".format(s.reminderHour, s.reminderMinute),
                    desc = null,
                    slider = {
                        Slider(
                            value = (s.reminderHour * 60 + s.reminderMinute).toFloat(),
                            onValueChange = { v ->
                                val m = v.toInt()
                                update { st -> st.copy(reminderHour = m / 60, reminderMinute = (m % 60) / 30 * 30) }
                            },
                            onValueChangeFinished = { ReminderScheduler.schedule(ctx, s.reminderHour, s.reminderMinute) },
                            valueRange = 0f..1439f, steps = 47,
                        )
                    },
                )
                }
            }
        }

        // --- 발음 ---
        GroupHeader(5, Icons.AutoMirrored.Outlined.VolumeUp, "발음")
        SettingsGroup {
            SliderRow(
                title = "읽어 주는 속도",
                value = "%.2fx".format(s.ttsRate),
                desc = "예문과 단어를 읽어 줄 때의 속도입니다",
                slider = {
                    Slider(
                        value = s.ttsRate,
                        onValueChange = { update { st -> st.copy(ttsRate = it) }; c.tts.rate = it },
                        valueRange = 0.6f..1.3f, steps = 13,
                    )
                },
            )
        }

        // --- 백업 ---
        GroupHeader(3, Icons.Outlined.Backup, "백업")
        SettingsGroup { BackupSection() }

        // --- 정보 ---
        GroupHeader(4, Icons.Outlined.Info, "정보")
        SettingsGroup {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("콘텐츠 버전", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    c.contentVersion,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            VSpace(12)
            Text(
                "단어 목록 출처: New General Service List, New Academic Word List, Business Service List " +
                    "(Browne, Culligan & Phillips; CC BY-SA 4.0) · The CEFR-J Wordlist Version 1.6 " +
                    "(Compiled by Yukio Tono, Tokyo University of Foreign Studies) · 교육부 고시 제2015-74호 " +
                    "영어과 교육과정 기본 어휘 목록. 뜻·예문·설명은 Wordy 자체 제작. " +
                    "간격 반복은 FSRS-6 알고리즘(open-spaced-repetition).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        VSpace(30)
    }
}

/**
 * 섹션 머리. 홈의 상세 목록과 같은 파스텔 아이콘 타일을 써서 두 화면이 한 앱으로 읽히게 한다.
 */
@Composable
private fun GroupHeader(tile: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(MaterialTheme.shapes.extraSmall)
                .background(tileBackgrounds[tile % tileBackgrounds.size]),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tileForegrounds[tile % tileForegrounds.size],
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(9.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * 슬라이더 한 줄. 제목과 현재 값을 같은 줄 양끝에 두어 지금 값이 얼마인지 바로 읽힌다.
 * 예전에는 "하루 새 단어 10개" 처럼 제목에 값을 섞어 써서 눈에 덜 띄었다.
 */
@Composable
private fun SliderRow(title: String, value: String, desc: String?, slider: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        if (desc != null) {
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        slider()
    }
}

/** 그룹 안 항목 사이 간격. 구분선 대신 여백으로 나눈다. */
@Composable
private fun RowGap() = VSpace(14)

@Composable
private fun ToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    divider: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
    if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * 학습 기록 내보내기·복원.
 *
 * content.db 는 앱에 내장되어 있어 백업 대상이 아니다. 기기를 바꾸거나 앱을 지웠다
 * 다시 깔 때 FSRS 일정과 숙달 단계를 그대로 옮기기 위한 기능이다.
 */
@Composable
private fun BackupSection() {
    val c = LocalAppContainer.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<BackupManager.Backup?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            message = runCatching {
                val out = ctx.contentResolver.openOutputStream(uri) ?: error("파일을 열 수 없습니다")
                out.use { c.backup.export(it, appVersion(ctx), c.contentVersion, c.now()) }
            }.fold(
                { "내보냈습니다. 카드 ${it.cards}개, 복습 기록 ${it.reviews}개, 단어 ${it.words}개." },
                { "내보내기 실패: ${it.message}" },
            )
            busy = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val input = runCatching { ctx.contentResolver.openInputStream(uri) ?: error("파일을 열 수 없습니다") }
            input.fold(
                { stream -> stream.use { c.backup.peek(it) }.fold({ pending = it }, { message = "읽기 실패: ${it.message}" }) },
                { message = "읽기 실패: ${it.message}" },
            )
        }
    }

    Text("학습 기록(복습 일정·숙달 단계·북마크)을 JSON 파일로 저장합니다. 단어 콘텐츠는 앱에 들어 있어 백업하지 않습니다.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    VSpace(8)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { exportLauncher.launch(BackupManager.fileName(c.now())) }, enabled = !busy) { Text("내보내기") }
        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }, enabled = !busy) { Text("복원") }
    }
    message?.let {
        VSpace(6)
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }

    pending?.let { backup ->
        val d = java.time.Instant.ofEpochMilli(backup.exportedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("학습 기록을 복원할까요?") },
            text = {
                Text(
                    "백업 날짜: ${d.year}년 ${d.monthValue}월 ${d.dayOfMonth}일\n" +
                        "카드 ${backup.cards.size}개 · 복습 기록 ${backup.reviewLogs.size}개 · 단어 ${backup.wordStatus.size}개\n\n" +
                        "지금 기기에 있는 학습 기록은 모두 지워지고 이 백업으로 바뀝니다. 되돌릴 수 없습니다."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null; busy = true
                    scope.launch {
                        message = runCatching { c.backup.restore(backup) }.fold(
                            { "복원했습니다. 카드 ${it.cards}개, 복습 기록 ${it.reviews}개." },
                            { "복원 실패: ${it.message}" },
                        )
                        busy = false
                    }
                }) { Text("복원") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("취소") } },
        )
    }
}

private fun appVersion(ctx: android.content.Context): String =
    runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?" }.getOrDefault("?")

/**
 * 설정 한 묶음. 항목이 배경 없이 죽 늘어서면 어디까지가 한 그룹인지 읽히지 않는다.
 * 카드로 감싸면 "학습 / 알림 / 발음 / 백업" 경계가 눈으로 구분된다.
 */
@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), content = content)
    }
}
