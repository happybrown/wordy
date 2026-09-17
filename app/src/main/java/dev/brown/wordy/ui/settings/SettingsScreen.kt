package dev.brown.wordy.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Spellcheck
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.graphics.Color
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

    // 값을 바꾸는 일은 시트에서 한다. 슬라이더를 화면에 죽 늘어놓으면
    // 지금 값이 무엇인지 읽기 전에 조절기부터 눈에 들어와 화면이 복잡해진다.
    var sheet by remember { mutableStateOf<SettingSheet?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        VSpace(20)
        Text("설정", style = MaterialTheme.typography.headlineMedium)

        // 지금 상태를 한 줄로 요약해 맨 위에 둔다. 설정을 열었을 때
        // "내가 어떻게 해 두었더라" 가 먼저 답이 된다.
        VSpace(14)
        SummaryCard(level = s.currentLevel.ko, daily = s.dailyNewWords, core = s.coreEnabled)

        GroupLabel("학습")
        SettingsCard {
            NavRow(
                tile = 0,
                icon = Icons.Outlined.School,
                title = "현재 레벨",
                value = s.currentLevel.ko,
                onClick = { sheet = SettingSheet.LevelPick },
            )
            RowLine()
            NavRow(
                tile = 2,
                icon = Icons.Outlined.Today,
                title = "하루 새 단어",
                value = if (s.dailyNewWords == 0) "복습만" else "${s.dailyNewWords}개",
                onClick = { sheet = SettingSheet.DailyGoal },
            )
            RowLine()
            SwitchRow(
                tile = 5,
                icon = Icons.Outlined.ChatBubbleOutline,
                title = "핵심 회화 어휘",
                desc = if (s.coreEnabled) "새 단어의 ${(s.coreRatio * 100).toInt()}%를 Core 에서" else "꺼짐",
                checked = s.coreEnabled,
                onChange = { update { st -> st.copy(coreEnabled = it) } },
                onClick = if (s.coreEnabled) ({ sheet = SettingSheet.CoreRatio }) else null,
            )
            RowLine()
            NavRow(
                tile = 3,
                icon = Icons.Outlined.Bolt,
                title = "복습 강도",
                value = "${(s.desiredRetention * 100).toInt()}%",
                onClick = { sheet = SettingSheet.Retention },
            )
            RowLine()
            SwitchRow(
                tile = 1,
                icon = Icons.Outlined.Spellcheck,
                title = "엄격한 타이핑 판정",
                desc = "1글자 오타도 오답으로",
                checked = s.strictTyping,
                onChange = { update { st -> st.copy(strictTyping = it) } },
                onClick = null,
            )
        }

        GroupLabel("알림과 발음")
        SettingsCard {
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    update { st -> st.copy(reminderEnabled = true) }
                    ReminderScheduler.schedule(ctx, s.reminderHour, s.reminderMinute)
                }
            }
            SwitchRow(
                tile = 1,
                icon = Icons.Outlined.Notifications,
                title = "매일 복습 알림",
                desc = if (s.reminderEnabled) "%02d:%02d 에 알려 줍니다".format(s.reminderHour, s.reminderMinute) else "꺼짐",
                checked = s.reminderEnabled,
                onChange = { on ->
                    if (!on) {
                        update { st -> st.copy(reminderEnabled = false) }; ReminderScheduler.cancel(ctx)
                    } else if (Build.VERSION.SDK_INT >= 33 && !ReminderScheduler.hasPermission(ctx)) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        update { st -> st.copy(reminderEnabled = true) }
                        ReminderScheduler.schedule(ctx, s.reminderHour, s.reminderMinute)
                    }
                },
                onClick = if (s.reminderEnabled) ({ sheet = SettingSheet.ReminderTime }) else null,
            )
            RowLine()
            NavRow(
                tile = 5,
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                title = "읽어 주는 속도",
                value = "%.2fx".format(s.ttsRate),
                onClick = { sheet = SettingSheet.TtsRate },
            )
        }

        GroupLabel("데이터")
        SettingsCard { BackupSection() }

        GroupLabel("정보")
        SettingsCard {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("콘텐츠 버전", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(c.contentVersion, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RowLine()
            Text(
                "단어 목록 출처: New General Service List, New Academic Word List, Business Service List " +
                    "(Browne, Culligan & Phillips; CC BY-SA 4.0) · The CEFR-J Wordlist Version 1.6 " +
                    "(Compiled by Yukio Tono, Tokyo University of Foreign Studies) · 교육부 고시 제2015-74호 " +
                    "영어과 교육과정 기본 어휘 목록. 뜻·예문·설명은 Wordy 자체 제작. " +
                    "간격 반복은 FSRS-6 알고리즘(open-spaced-repetition).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
        VSpace(30)
    }

    when (sheet) {
        SettingSheet.LevelPick -> PickerSheet("현재 레벨", "새 단어를 어느 레벨에서 뽑을지 정합니다", { sheet = null }) {
            Level.entries.forEach { lvl ->
                ChoiceRow(lvl.ko, selected = s.currentLevel == lvl) {
                    update { it.copy(currentLevel = lvl) }; sheet = null
                }
            }
        }
        SettingSheet.DailyGoal -> SliderSheet(
            title = "하루 새 단어",
            value = if (s.dailyNewWords == 0) "복습만 합니다" else "${s.dailyNewWords}개",
            desc = "복습은 알고리즘이 따로 잡아 줍니다",
            onDismiss = { sheet = null },
        ) {
            Slider(
                value = s.dailyNewWords.toFloat(),
                onValueChange = { update { st -> st.copy(dailyNewWords = it.toInt()) } },
                valueRange = 0f..40f, steps = 39,
            )
        }
        SettingSheet.CoreRatio -> SliderSheet(
            title = "Core 비율",
            value = "${(s.coreRatio * 100).toInt()}%",
            desc = "새 단어 중 get, take 같은 기본 어휘의 비중",
            onDismiss = { sheet = null },
        ) {
            Slider(
                value = s.coreRatio,
                onValueChange = { update { st -> st.copy(coreRatio = it) } },
                valueRange = 0.1f..0.9f, steps = 7,
            )
        }
        SettingSheet.Retention -> SliderSheet(
            title = "복습 강도",
            value = "${(s.desiredRetention * 100).toInt()}%",
            desc = "높이면 복습이 잦아지고, 낮추면 복습은 줄지만 더 자주 잊습니다. 기본 90%.",
            onDismiss = { sheet = null },
        ) {
            Slider(
                value = s.desiredRetention,
                onValueChange = { update { st -> st.copy(desiredRetention = it) } },
                valueRange = 0.8f..0.95f, steps = 14,
            )
        }
        SettingSheet.ReminderTime -> SliderSheet(
            title = "알림 시각",
            value = "%02d:%02d".format(s.reminderHour, s.reminderMinute),
            desc = "30분 단위로 정할 수 있습니다",
            onDismiss = { sheet = null },
        ) {
            Slider(
                value = (s.reminderHour * 60 + s.reminderMinute).toFloat(),
                onValueChange = { v ->
                    val m = v.toInt()
                    update { st -> st.copy(reminderHour = m / 60, reminderMinute = (m % 60) / 30 * 30) }
                },
                onValueChangeFinished = { ReminderScheduler.schedule(ctx, s.reminderHour, s.reminderMinute) },
                valueRange = 0f..1439f, steps = 47,
            )
        }
        SettingSheet.TtsRate -> SliderSheet(
            title = "읽어 주는 속도",
            value = "%.2fx".format(s.ttsRate),
            desc = "예문과 단어를 읽어 줄 때의 속도입니다",
            onDismiss = { sheet = null },
        ) {
            Slider(
                value = s.ttsRate,
                onValueChange = { update { st -> st.copy(ttsRate = it) }; c.tts.rate = it },
                valueRange = 0.6f..1.3f, steps = 13,
            )
        }
        null -> Unit
    }
}

/** 시트로 조절하는 항목들. 화면에는 값만 보이고 조절기는 시트 안에 둔다. */
private enum class SettingSheet { LevelPick, DailyGoal, CoreRatio, Retention, ReminderTime, TtsRate }

/** 지금 설정을 한 줄로 요약. 설정을 여는 이유의 절반은 "내가 뭘로 해 뒀지" 다. */
@Composable
private fun SummaryCard(level: String, daily: Int, core: Boolean) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            SummaryCell(level, "레벨", Modifier.weight(1f))
            SummaryCell(if (daily == 0) "복습만" else "$daily", "하루 새 단어", Modifier.weight(1f))
            SummaryCell(if (core) "켜짐" else "꺼짐", "Core 트랙", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** 카드 위 작은 분류 이름. 아이콘 없이 글자만 둬 카드 안의 아이콘과 경쟁하지 않는다. */
@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        Modifier.padding(start = 4.dp, top = 26.dp, bottom = 9.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(content = content)
    }
}

/** 행 사이 선. 아이콘 폭만큼 들여써서 목록이 끊겨 보이지 않게 한다. */
@Composable
private fun RowLine() {
    Box(
        Modifier.padding(start = 64.dp).fillMaxWidth().height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun RowIcon(tile: Int, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        Modifier.size(34.dp).clip(MaterialTheme.shapes.extraSmall)
            .background(tileBackgrounds[tile % tileBackgrounds.size]),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tileForegrounds[tile % tileForegrounds.size],
            modifier = Modifier.size(18.dp),
        )
    }
}

/** 눌러서 시트를 여는 행. 오른쪽에 현재 값과 화살표를 둔다. */
@Composable
private fun NavRow(
    tile: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowIcon(tile, icon)
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * 스위치가 달린 행. [onClick] 이 있으면 행 본문을 눌러 상세 시트를 열 수 있다.
 * 스위치와 행 클릭이 같은 자리에서 다른 일을 하므로 스위치에만 토글을 맡긴다.
 */
@Composable
private fun SwitchRow(
    tile: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    onClick: (() -> Unit)?,
) {
    val inner: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowIcon(tile, icon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
    if (onClick != null) Surface(onClick = onClick, color = Color.Transparent) { inner() } else inner()
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PickerSheet(
    title: String,
    desc: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            VSpace(3)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            VSpace(16)
            content()
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

/** 값 하나를 슬라이더로 조절하는 시트. 큰 숫자를 먼저 보여 주고 그 아래에서 조절한다. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SliderSheet(
    title: String,
    value: String,
    desc: String,
    onDismiss: () -> Unit,
    slider: @Composable () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 30.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            VSpace(4)
            Text(value, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            VSpace(6)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            VSpace(14)
            slider()
        }
    }
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
