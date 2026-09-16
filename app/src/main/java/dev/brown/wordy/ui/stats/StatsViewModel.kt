package dev.brown.wordy.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brown.wordy.di.AppContainer
import dev.brown.wordy.domain.mastery.streakOf
import dev.brown.wordy.domain.model.Mastery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 하루치 막대 하나. reviewed 가 0 이면 학습하지 않은 날. */
data class DayBar(val date: LocalDate, val reviewed: Int, val correct: Int, val newLearned: Int, val seconds: Int)

data class WeakWord(val wordId: String, val headword: String, val ko: String, val lapses: Int, val stabilityDays: Double)

data class StatsUiState(
    val loading: Boolean = true,
    val totalReviews: Int = 0,
    /** 최근 30일 정답률. 복습이 한 번도 없으면 null. */
    val recentAccuracy: Double? = null,
    val reviews30d: Int = 0,
    val seconds30d: Int = 0,
    val activeDays30d: Int = 0,
    val streak: Int = 0,
    val days: List<DayBar> = emptyList(),
    val mastery: Map<Mastery, Int> = emptyMap(),
    val introduced: Int = 0,
    val weakWords: List<WeakWord> = emptyList(),
    /** 숙달까지 평균 며칠 걸렸는지. 숙달 단어가 없으면 null. */
    val avgDaysToMaster: Double? = null,
)

class StatsViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val daily = c.progress.allDaily().associateBy { it.date }

            // 30일 창을 날짜로 채운다. 기록이 없는 날도 0 으로 넣어야 그래프에 빈 칸이 보인다.
            val days = (29L downTo 0L).map { back ->
                val d = today.minusDays(back)
                val s = daily[d.toString()]
                DayBar(d, s?.reviewed ?: 0, s?.correct ?: 0, s?.newLearned ?: 0, s?.seconds ?: 0)
            }

            val reviews30d = days.sumOf { it.reviewed }
            val correct30d = days.sumOf { it.correct }
            // 통계는 한 번만 읽으면 되므로 Flow 를 구독하지 않고 현재 값만 쓴다
            val statuses = c.progress.observeAllStatuses().first()

            val masteryCounts = Mastery.entries.associateWith { m -> statuses.count { it.mastery == m } }
            val mastered = statuses.filter { it.masteredAt != null && it.introducedAt != null }
            val avgDays = mastered
                .map { (it.masteredAt!! - it.introducedAt!!).toDouble() / 86_400_000.0 }
                .takeIf { it.isNotEmpty() }?.average()

            val weakCards = c.progress.weakCards(10)
            val weakWordsById = c.words.words(weakCards.map { it.wordId }).associateBy { it.id }
            val weak = weakCards.mapNotNull { card ->
                val w = weakWordsById[card.wordId] ?: return@mapNotNull null
                WeakWord(card.wordId, w.headword, w.senses.firstOrNull()?.ko.orEmpty(), card.lapses, card.stability)
            }

            _state.value = StatsUiState(
                loading = false,
                totalReviews = c.progress.totalReviews(),
                recentAccuracy = if (reviews30d > 0) correct30d.toDouble() / reviews30d else null,
                reviews30d = reviews30d,
                seconds30d = days.sumOf { it.seconds },
                activeDays30d = days.count { it.reviewed > 0 || it.newLearned > 0 },
                streak = streakOf(daily.filterValues { it.reviewed > 0 || it.newLearned > 0 }.keys, today),
                days = days,
                mastery = masteryCounts,
                introduced = statuses.count { it.mastery.level >= Mastery.MET.level },
                weakWords = weak,
                avgDaysToMaster = avgDays,
            )
        }
    }
}
