package dev.brown.wordy.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brown.wordy.di.AppContainer
import dev.brown.wordy.domain.model.Level
import dev.brown.wordy.domain.model.Mastery
import dev.brown.wordy.domain.model.Unit
import dev.brown.wordy.domain.model.Word
import dev.brown.wordy.domain.model.WordStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class UnitRow(val unit: Unit, val familiar: Int, val met: Int)
data class WordRow(val word: Word, val mastery: Mastery, val bookmarked: Boolean)

data class BrowseUiState(
    val loading: Boolean = true,
    val tab: Int = 0, // 0 = Core, 1..5 = levels
    val units: Map<Level, List<UnitRow>> = emptyMap(),
    val coreWords: List<WordRow> = emptyList(),
    val query: String = "",
    val results: List<WordRow> = emptyList(),
)

/**
 * @param requestedTab 홈에서 지정해 들어온 탭. null 이면 현재 레벨 탭을 연다.
 *
 * 화면 쪽 LaunchedEffect 로 탭을 지정하면 안 된다. init 이 `c.settings.current()`
 * 를 await 하는 동안 LaunchedEffect 가 먼저 selectTab 을 부르고, await 가 끝나면
 * init 이 그 값을 현재 레벨로 덮어쓴다. 타이밍에 따라 결과가 달라지는 경쟁 조건이라
 * 디버그 빌드에서는 통과하고 릴리스에서는 실패했다. 생성 시점에 받아 해결한다.
 */
class BrowseViewModel(
    private val c: AppContainer,
    private val requestedTab: Int? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(BrowseUiState(tab = requestedTab ?: 0))
    val state: StateFlow<BrowseUiState> = _state
    private var statuses: Map<String, WordStatus> = emptyMap()
    private var unitWordIds: Map<String, List<String>> = emptyMap()

    init {
        viewModelScope.launch {
            // 지정 탭이 없을 때만 현재 레벨을 기본값으로 쓴다.
            if (requestedTab == null) {
                _state.value = _state.value.copy(tab = c.settings.current().currentLevel.ordinal + 1)
            }
            val unitsByLevel = Level.entries.associateWith { c.words.units(it) }
            unitWordIds = unitsByLevel.values.flatten().associate { u -> u.id to c.words.wordsByUnit(u.id).map { it.id } }
            val core = c.words.words(c.words.coreWordIds())
            c.progress.observeAllStatuses().collectLatest { list ->
                statuses = list.associateBy { it.wordId }
                _state.value = _state.value.copy(
                    loading = false,
                    units = unitsByLevel.mapValues { (_, units) -> units.map { u -> val ids = unitWordIds[u.id].orEmpty(); UnitRow(u, ids.count { m(it) >= 3 }, ids.count { m(it) >= 1 }) } },
                    coreWords = core.map { row(it) },
                    results = _state.value.results.map { row(it.word) },
                )
            }
        }
    }

    private fun m(id: String) = statuses[id]?.mastery?.level ?: 0
    private fun row(w: Word) = WordRow(w, statuses[w.id]?.mastery ?: Mastery.NEW, statuses[w.id]?.isBookmarked ?: false)

    fun selectTab(i: Int) { _state.value = _state.value.copy(tab = i) }

    fun search(q: String) {
        _state.value = _state.value.copy(query = q)
        if (q.isBlank()) { _state.value = _state.value.copy(results = emptyList()); return }
        viewModelScope.launch { _state.value = _state.value.copy(results = c.words.search(q).map { row(it) }) }
    }
}
