package dev.brown.wordy.domain.session

import dev.brown.wordy.data.prefs.SettingsRepository
import dev.brown.wordy.data.repo.ProgressRepository
import dev.brown.wordy.data.repo.WordRepository
import dev.brown.wordy.domain.model.Card
import kotlinx.coroutines.flow.first

/** 오늘 세션 계획: 복습할 카드 + 새로 배울 단어 id */
data class SessionPlan(val dueCards: List<Card>, val newWordIds: List<String>) {
    val isEmpty get() = dueCards.isEmpty() && newWordIds.isEmpty()
}

class SessionBuilder(
    private val words: WordRepository,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
) {
    /**
     * @param extraNew 사용자가 "더 배우기"를 눌렀을 때 오늘 목표를 넘어 추가할 신규 수
     */
    suspend fun build(now: Long, maxDue: Int = 150, extraNew: Int = 0): SessionPlan {
        val s = settings.current()
        val due = progress.dueCards(now, maxDue)
        val learnedToday = progress.observeDaily(ProgressRepository.today(now)).first()?.newLearned ?: 0
        val remaining = (s.dailyNewWords - learnedToday).coerceAtLeast(0) + extraNew
        val newIds = pickNewWords(remaining, s.currentLevel, s.coreEnabled, s.coreRatio)
        return SessionPlan(due, newIds)
    }

    suspend fun pickNewWords(count: Int, level: dev.brown.wordy.domain.model.Level, coreEnabled: Boolean, coreRatio: Float): List<String> {
        if (count <= 0) return emptyList()
        val introduced = progress.introducedWordIds()
        val coreTarget = if (coreEnabled) Math.round(count * coreRatio) else 0
        val core = if (coreTarget > 0) words.coreWordIds().filter { it !in introduced }.take(coreTarget) else emptyList()
        val levelIds = words.wordIdsByLevel(level).filter { it !in introduced && it !in core }
        var levelPick = levelIds.take(count - core.size)
        // 현재 레벨을 다 배웠으면 다음 레벨에서 채운다
        if (levelPick.size < count - core.size) {
            for (next in dev.brown.wordy.domain.model.Level.entries.filter { it.ordinal > level.ordinal }) {
                if (levelPick.size >= count - core.size) break
                levelPick = levelPick + words.wordIdsByLevel(next).filter { it !in introduced && it !in core && it !in levelPick }.take(count - core.size - levelPick.size)
            }
        }
        // Core 를 다 배웠으면 레벨 단어로 채운다
        val shortfall = count - core.size - levelPick.size
        val more = if (shortfall > 0) words.coreWordIds().filter { it !in introduced && it !in core }.take(shortfall) else emptyList()
        return interleave(levelPick, core + more)
    }

    /** 레벨 단어와 Core 단어를 골고루 섞는다 (비율 유지) */
    private fun interleave(a: List<String>, b: List<String>): List<String> {
        if (a.isEmpty()) return b; if (b.isEmpty()) return a
        val out = ArrayList<String>(a.size + b.size)
        var i = 0; var j = 0
        while (i < a.size || j < b.size) {
            val ratio = (i + 1).toDouble() / a.size
            val ratioB = (j + 1).toDouble() / b.size
            if (j >= b.size || (i < a.size && ratio <= ratioB)) out += a[i++] else out += b[j++]
        }
        return out
    }
}
