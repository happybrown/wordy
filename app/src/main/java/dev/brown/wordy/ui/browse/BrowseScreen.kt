package dev.brown.wordy.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Surface
import dev.brown.wordy.ui.components.EmptyMessage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.brown.wordy.domain.model.Level
import dev.brown.wordy.ui.components.LoadingBox
import dev.brown.wordy.ui.components.MasteryDot
import dev.brown.wordy.ui.components.ProgressBar
import dev.brown.wordy.ui.components.appViewModel
import dev.brown.wordy.ui.theme.masteryColors

/**
 * 단어장. [initialTab] 으로 열 탭을 지정한다.
 *
 * 탭 번호를 ViewModel 생성 인자로 넘긴다. LaunchedEffect 로 넘기면 ViewModel 의
 * init 이 설정을 await 하는 사이에 순서가 뒤집혀 지정이 무시된다.
 * appViewModel 의 key 에도 탭을 넣어, 다른 탭으로 다시 들어오면 새 인스턴스를 받는다.
 */
@Composable
fun BrowseScreen(
    initialTab: BrowseTab?,
    onOpenUnit: (unitId: String, title: String) -> Unit,
    onOpenWord: (String) -> Unit,
) {
    val requested: Int? = when (initialTab) {
        BrowseTab.Core -> 0
        is BrowseTab.OfLevel -> initialTab.level.ordinal + 1
        null -> null
    }
    val vm = appViewModel(key = "browse-${requested ?: "default"}") { BrowseViewModel(it, requested) }
    val s by vm.state.collectAsStateWithLifecycle()
    if (s.loading) { LoadingBox(); return }

    Column(Modifier.fillMaxSize()) {
        // 검색창은 테두리 대신 채운 알약 모양. 요즘 앱의 기본 검색 인상이고
        // 테두리가 사라지면 아래 탭·목록과 경쟁하지 않는다.
        TextField(
            value = s.query, onValueChange = vm::search, singleLine = true,
            placeholder = { Text("영어 또는 한국어로 검색") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (s.query.isNotEmpty()) IconButton(onClick = { vm.search("") }) { Icon(Icons.Default.Clear, "지우기") } },
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        )
        if (s.query.isNotBlank()) {
            WordList(s.results, onOpenWord, emptyText = "검색 결과가 없어요")
            return
        }
        val tabs = listOf("Core") + Level.entries.map { it.ko }
        PrimaryScrollableTabRow(selectedTabIndex = s.tab, edgePadding = 12.dp) {
            tabs.forEachIndexed { i, t -> Tab(selected = s.tab == i, onClick = { vm.selectTab(i) }, text = { Text(t) }) }
        }
        if (s.tab == 0) {
            WordList(s.coreWords, onOpenWord, emptyText = "Core 단어가 아직 없어요", showCoreBadge = false)
        } else {
            val level = Level.entries[s.tab - 1]
            val units = s.units[level].orEmpty()
            if (units.isEmpty()) { Text("이 레벨의 콘텐츠가 아직 없어요.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant); return }
            // 레벨 전체 진행을 맨 위에 한 줄로 요약한다. 유닛 카드만 늘어놓으면
            // 이 레벨을 얼마나 했는지 알려면 카드를 다 세어 봐야 한다.
            val totalWords = units.sumOf { it.unit.wordCount }
            val totalFamiliar = units.sumOf { it.familiar }
            val totalMet = units.sumOf { it.met }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 14.dp, bottom = 20.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "level-summary") {
                    LevelSummary(level.ko, totalFamiliar, totalMet, totalWords)
                }
                items(units, key = { it.unit.id }) { row ->
                    UnitCard(
                        title = row.unit.title,
                        familiar = row.familiar,
                        met = row.met,
                        total = row.unit.wordCount,
                        onClick = { onOpenUnit(row.unit.id, row.unit.title) },
                    )
                }
            }
        }
    }
}

/**
 * 단어 목록.
 *
 * @param showCoreBadge Core 탭에서는 모든 항목이 Core 라서 배지가 정보를 주지 않는다.
 *   레벨 탭에서만 켜서 "이 단어는 Core 이기도 하다" 를 알린다.
 */
@Composable
fun WordList(
    rows: List<WordRow>,
    onOpenWord: (String) -> Unit,
    emptyText: String,
    showCoreBadge: Boolean = true,
) {
    if (rows.isEmpty()) {
        EmptyMessage(emptyText)
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
        itemsIndexed(rows, key = { _, r -> r.word.id }) { i, r ->
            Column {
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenWord(r.word.id) }
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    MasteryDot(r.mastery, describe = true)
                    Column(Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(r.word.headword, style = MaterialTheme.typography.titleMedium)
                            Text(
                                r.word.primarySense.pos.ko,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (showCoreBadge && r.word.isCore) CoreBadge()
                        }
                        Text(
                            r.word.senses.take(3).joinToString(" · ") { it.ko },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    // 북마크 여부가 아이콘으로만 표시되므로 설명이 없으면 스크린 리더로는 구분할 수 없다
                    if (r.bookmarked) {
                        Icon(Icons.Default.Bookmark, "북마크함", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
                // 행 사이 선. 점 폭만큼 들여써서 목록이 끊겨 보이지 않게 한다.
                if (i != rows.lastIndex) {
                    Box(
                        Modifier.padding(start = 45.dp, end = 20.dp).fillMaxWidth().height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        }
    }
}

/** Core 배지. 글자만 두면 품사 라벨과 섞여 읽히므로 알약으로 감싼다. */
@Composable
private fun CoreBadge() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
        Text(
            "Core",
            Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

/** 레벨 전체 진행 요약. 유닛 카드 위에 한 장만 둔다. */
@Composable
private fun LevelSummary(levelName: String, familiar: Int, met: Int, total: Int) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "$levelName 단어 ${total}개",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    if (met == 0) "아직 시작하지 않았어요" else "익숙 $familiar · 배우는 중 ${met - familiar}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                )
            }
            ProgressRing(
                progress = if (total == 0) 0f else familiar.toFloat() / total,
                partial = if (total == 0) 0f else met.toFloat() / total,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/**
 * 유닛 카드. 흰 카드에 오른쪽 진행 원을 둔다.
 * 예전에는 회색 카드에 회색 막대여서 카드끼리 구분이 안 되고 진행률도 묻혔다.
 */
@Composable
private fun UnitCard(title: String, familiar: Int, met: Int, total: Int, onClick: () -> Unit) {
    val progress = if (total == 0) 0f else familiar.toFloat() / total
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    // 0 이 셋 늘어서면 읽히지 않는다. 시작 전에는 개수만 말한다.
                    if (met == 0) "${total}단어" else "익숙 $familiar · 만남 $met / $total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            ProgressRing(
                progress = progress,
                partial = if (total == 0) 0f else met.toFloat() / total,
                color = masteryColors[3],
                size = 34,
            )
        }
    }
}

/**
 * 진행 원. 막대보다 좁은 자리에서 잘 읽히고 0%일 때도 링이 보인다.
 *
 * @param progress '익숙' 이상에 오른 비율. 진한 호로 그린다.
 * @param partial 한 번이라도 배운 비율. 옅은 호로 그려 "하는 중" 을 보여 준다.
 *   이게 없으면 7개를 배우는 중인데도 링이 완전히 비어 아무것도 안 한 것처럼 보인다.
 */
@Composable
private fun ProgressRing(progress: Float, partial: Float = 0f, color: Color, size: Int = 40) {
    val done = progress.coerceIn(0f, 1f)
    val started = partial.coerceIn(done, 1f)
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val stroke = size * 0.11f * density
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
            val offset = androidx.compose.ui.geometry.Offset(inset, inset)
            val style = androidx.compose.ui.graphics.drawscope.Stroke(
                stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            // 바닥 링
            drawArc(color.copy(alpha = 0.2f), -90f, 360f, false, offset, arcSize, style = style)
            // 배우는 중
            if (started > 0f) {
                drawArc(color.copy(alpha = 0.45f), -90f, 360f * started, false, offset, arcSize, style = style)
            }
            // 익숙
            if (done > 0f) {
                drawArc(color, -90f, 360f * done, false, offset, arcSize, style = style)
            }
        }
        // 숫자는 익숙 비율만 쓴다. 0% 일 때는 개수인지 퍼센트인지 모호해 생략한다.
        if (done > 0f) {
            Text("${(done * 100).toInt()}", style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

/** 단어장에서 열 탭. Core 와 레벨을 한 타입으로 묶어 null 의 뜻을 하나로 만든다. */
sealed interface BrowseTab {
    data object Core : BrowseTab
    data class OfLevel(val level: Level) : BrowseTab
}
