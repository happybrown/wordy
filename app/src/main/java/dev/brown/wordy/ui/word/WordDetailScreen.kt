package dev.brown.wordy.ui.word

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.brown.wordy.data.db.progress.ReviewLogEntity
import dev.brown.wordy.di.AppContainer
import dev.brown.wordy.domain.model.Card as DomainCard
import dev.brown.wordy.domain.model.Mastery
import dev.brown.wordy.domain.model.Word
import dev.brown.wordy.domain.model.WordStatus
import dev.brown.wordy.domain.srs.CardState
import dev.brown.wordy.ui.components.LoadingBox
import dev.brown.wordy.ui.components.MasteryChip
import dev.brown.wordy.ui.components.SectionTitle
import dev.brown.wordy.ui.components.VSpace
import dev.brown.wordy.ui.components.WordCard
import dev.brown.wordy.ui.components.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WordDetailState(val word: Word? = null, val status: WordStatus? = null, val cards: List<DomainCard> = emptyList(), val logs: List<ReviewLogEntity> = emptyList(), val missing: Boolean = false)

class WordDetailViewModel(private val c: AppContainer, private val wordId: String) : ViewModel() {
    private val _state = MutableStateFlow(WordDetailState())
    val state: StateFlow<WordDetailState> = _state
    init {
        viewModelScope.launch {
            val w = c.words.word(wordId)
            if (w == null) { _state.value = WordDetailState(missing = true); return@launch }
            _state.value = _state.value.copy(word = w)
            c.progress.observeStatus(wordId).collectLatest { st -> _state.value = _state.value.copy(status = st, cards = c.progress.cardsForWord(wordId), logs = c.progress.recentLogs(wordId, 15)) }
        }
    }
    fun toggleBookmark() = viewModelScope.launch { c.progress.setBookmark(wordId, !(state.value.status?.isBookmarked ?: false), c.now()) }
    fun saveNote(note: String) = viewModelScope.launch { c.progress.setNote(wordId, note, c.now()) }
    fun introduceNow() = viewModelScope.launch { state.value.word?.let { c.engine.introduce(it, c.now()) } }
    fun speak(text: String) = c.tts.speak(text)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(wordId: String, onBack: () -> Unit, onPractice: (String) -> Unit) {
    val vm = appViewModel(key = "word-$wordId") { WordDetailViewModel(it, wordId) }
    val s by vm.state.collectAsStateWithLifecycle()
    val word = s.word
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(word?.headword ?: "") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") } },
            actions = { IconButton(onClick = vm::toggleBookmark) { Icon(if (s.status?.isBookmarked == true) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "북마크") } },
        )
    }) { p ->
        if (s.missing) { Text("단어를 찾을 수 없어요.", Modifier.padding(p).padding(24.dp)); return@Scaffold }
        if (word == null) { LoadingBox(Modifier.padding(p)); return@Scaffold }
        Column(Modifier.fillMaxSize().padding(p).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                MasteryChip(s.status?.mastery ?: Mastery.NEW)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedButton(onClick = { onPractice(wordId) }) { Text("연습하기") }
                    if (s.cards.isEmpty()) Button(onClick = vm::introduceNow) { Text("학습에 추가") }
                }
            }
            VSpace(12)
            WordCard(word, onSpeak = vm::speak)
            if (s.cards.isNotEmpty()) {
                SectionTitle("학습 상태")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        s.cards.sortedBy { it.due }.forEach { card ->
                            val senseKo = card.senseId?.let { id -> word.senses.firstOrNull { it.id == id }?.ko }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(card.skill.ko + (senseKo?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodyMedium)
                                Text(describe(card), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            SectionTitle("메모")
            var note by remember(s.status?.note) { mutableStateOf(s.status?.note ?: "") }
            OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("나만의 연상법, 헷갈린 이유…") }, minLines = 2)
            if (note != (s.status?.note ?: "")) Button(onClick = { vm.saveNote(note) }, modifier = Modifier.padding(top = 6.dp)) { Text("메모 저장") }
            if (s.logs.isNotEmpty()) {
                SectionTitle("최근 복습")
                val fmt = SimpleDateFormat("M/d HH:mm", Locale.KOREA)
                s.logs.forEach { l ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${fmt.format(Date(l.reviewedAt))}  ${l.exerciseType.lowercase().replace('_', ' ')}", style = MaterialTheme.typography.bodySmall)
                        Text(if (l.wasCorrect) "정답" else "오답", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = if (l.wasCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                    HorizontalDivider()
                }
            }
            VSpace(32)
        }
    }
}

private fun describe(card: DomainCard): String {
    val now = System.currentTimeMillis()
    val dueIn = (card.due - now) / 86_400_000.0
    val state = when (card.state) { CardState.NEW -> "새 카드"; CardState.LEARNING -> "학습 중"; CardState.RELEARNING -> "재학습"; CardState.REVIEW -> "복습 (${"%.0f".format(card.stability)}일)" }
    val due = when { card.due <= now -> "지금"; dueIn < 1 -> "오늘"; else -> "${dueIn.toInt()}일 후" }
    return "$state · $due"
}
