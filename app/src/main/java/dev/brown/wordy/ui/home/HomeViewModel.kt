package dev.brown.wordy.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brown.wordy.data.repo.ProgressRepository
import dev.brown.wordy.di.AppContainer
import dev.brown.wordy.domain.mastery.streakOf
import dev.brown.wordy.domain.model.Level
import dev.brown.wordy.domain.model.Mastery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LevelProgress(val level: Level, val total: Int, val met: Int, val familiar: Int)

data class HomeUiState(
    val loading: Boolean = true,
    val contentVersion: String = "",
    val totalWords: Int = 0,
    val dailyGoal: Int = 10,
    val learnedToday: Int = 0,
    val reviewedToday: Int = 0,
    val dueNow: Int = 0,
    val dueTomorrow: Int = 0,
    val streak: Int = 0,
    val currentLevel: Level = Level.ELEM,
    val levels: List<LevelProgress> = emptyList(),
    val coreTotal: Int = 0,
    val coreFamiliar: Int = 0,
    val coreMet: Int = 0,
    val totalReviews: Int = 0,
    /** 최근 7일 학습량(복습+신규). 마지막 항목이 오늘. */
    val weekActivity: List<Int> = List(7) { 0 },
    /** 전 레벨에서 '익숙' 이상에 오른 단어 수. */
    val familiarTotal: Int = 0,
)

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init {
        viewModelScope.launch {
            val counts = c.words.countsByLevel()
            val coreIds = c.words.coreWordIds().toSet()
            val today = ProgressRepository.today(c.now())
            combine(c.settings.settings, c.progress.observeAllStatuses(), c.progress.observeDaily(today), c.progress.dueCount(c.now())) { s, statuses, daily, due ->
                val byLevel = Level.entries.map { lvl -> lvl to (counts[lvl] ?: 0) }
                val statusMap = statuses.associateBy { it.wordId }
                val levelProgress = byLevel.map { (lvl, total) ->
                    // 상태는 wordId 만 있으므로 레벨별 집계는 id 목록이 필요: 캐시된 레벨별 id 사용
                    val ids = levelIds(lvl)
                    LevelProgress(lvl, total, ids.count { statusMap[it]?.mastery?.level ?: 0 >= 1 }, ids.count { statusMap[it]?.mastery?.level ?: 0 >= Mastery.FAMILIAR.level })
                }
                HomeUiState(
                    loading = false, contentVersion = c.contentVersion, totalWords = counts.values.sum(),
                    dailyGoal = s.dailyNewWords, learnedToday = daily?.newLearned ?: 0, reviewedToday = daily?.reviewed ?: 0,
                    dueNow = due, currentLevel = s.currentLevel, levels = levelProgress,
                    coreTotal = coreIds.size, coreFamiliar = coreIds.count { statusMap[it]?.mastery?.level ?: 0 >= Mastery.FAMILIAR.level }, coreMet = coreIds.count { statusMap[it]?.mastery?.level ?: 0 >= 1 },
                    familiarTotal = statuses.count { it.mastery.level >= Mastery.FAMILIAR.level },
                )
            }.collectLatest { base ->
                val tomorrowStart = ProgressRepository.startOfDay(LocalDate.now().plusDays(1))
                val dueTomorrow = c.progress.dueBetween(c.now(), tomorrowStart + 86_400_000L)
                val allDaily = c.progress.allDaily()
                _state.value = base.copy(
                    dueTomorrow = dueTomorrow,
                    streak = streakOf(allDaily.filter { it.reviewed > 0 || it.newLearned > 0 }.map { it.date }.toSet(), LocalDate.now()),
                    totalReviews = c.progress.totalReviews(),
                    weekActivity = weekActivity(allDaily),
                )
            }
        }
    }

    private val levelIdCache = HashMap<Level, List<String>>()
    private suspend fun levelIds(level: Level): List<String> = levelIdCache.getOrPut(level) { c.words.wordIdsByLevel(level) }

    /** 최근 7일 학습량. 기록이 없는 날도 0 으로 채워야 막대 간격이 일정해진다. */
    private fun weekActivity(daily: List<dev.brown.wordy.domain.model.DailyStat>): List<Int> {
        val byDate = daily.associateBy { it.date }
        val today = LocalDate.now()
        return (6L downTo 0L).map { back ->
            val d = byDate[today.minusDays(back).toString()]
            (d?.reviewed ?: 0) + (d?.newLearned ?: 0)
        }
    }
}
