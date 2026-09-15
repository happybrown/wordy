package dev.brown.wordy.ui.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import dev.brown.wordy.domain.model.Mastery
import dev.brown.wordy.ui.components.LoadingBox
import dev.brown.wordy.ui.components.LocalAppContainer
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitScreen(unitId: String, title: String, onBack: () -> Unit, onOpenWord: (String) -> Unit) {
    val c = LocalAppContainer.current
    var rows by remember { mutableStateOf<List<WordRow>?>(null) }
    LaunchedEffect(unitId) {
        val words = c.words.wordsByUnit(unitId)
        c.progress.observeAllStatuses().collectLatest { list ->
            val map = list.associateBy { it.wordId }
            rows = words.map { WordRow(it, map[it.id]?.mastery ?: Mastery.NEW, map[it.id]?.isBookmarked ?: false) }
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") } }) }) { p ->
        Column(Modifier.fillMaxSize().padding(p)) {
            val r = rows
            if (r == null) LoadingBox() else WordList(r, onOpenWord, emptyText = "단어가 없어요")
        }
    }
}
