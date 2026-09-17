package dev.brown.wordy.ui.browse

import androidx.compose.foundation.clickable
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
            WordList(s.coreWords, onOpenWord, emptyText = "Core 단어가 아직 없어요")
        } else {
            val level = Level.entries[s.tab - 1]
            val units = s.units[level].orEmpty()
            if (units.isEmpty()) { Text("이 레벨의 콘텐츠가 아직 없어요.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant); return }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(units, key = { it.unit.id }) { row ->
                    Card(onClick = { onOpenUnit(row.unit.id, row.unit.title) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(row.unit.title, fontWeight = FontWeight.SemiBold)
                                Text("익숙 ${row.familiar} · 만남 ${row.met} / ${row.unit.wordCount}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            ProgressBar(if (row.unit.wordCount == 0) 0f else row.familiar.toFloat() / row.unit.wordCount, color = masteryColors[3])
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WordList(rows: List<WordRow>, onOpenWord: (String) -> Unit, emptyText: String) {
    if (rows.isEmpty()) { Text(emptyText, Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant); return }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) {
        items(rows, key = { it.word.id }) { r ->
            Row(
                Modifier.fillMaxWidth().clickable { onOpenWord(r.word.id) }.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MasteryDot(r.mastery, describe = true)
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(r.word.headword, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(r.word.primarySense.pos.ko, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (r.word.isCore) Text("Core", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Text(r.word.senses.take(3).joinToString(" · ") { it.ko }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                // 북마크 여부가 아이콘으로만 표시되므로 설명이 없으면 스크린 리더로는 구분할 수 없다
                if (r.bookmarked) Icon(Icons.Default.Bookmark, "북마크함", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** 단어장에서 열 탭. Core 와 레벨을 한 타입으로 묶어 null 의 뜻을 하나로 만든다. */
sealed interface BrowseTab {
    data object Core : BrowseTab
    data class OfLevel(val level: Level) : BrowseTab
}
