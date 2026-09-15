package dev.brown.wordy.domain.model

import dev.brown.wordy.domain.srs.CardState
import dev.brown.wordy.domain.srs.FsrsCard

/**
 * 학습 스킬. perSense 스킬은 sense 마다 카드가 하나씩, 나머지는 단어당 하나.
 * USE 는 Core 단어에만 만든다.
 */
enum class Skill(val perSense: Boolean, val ko: String) {
    RECOGNIZE(true, "뜻 인식"),
    CONTEXT(true, "문맥"),
    RECALL(false, "영어 회상"),
    USE(false, "활용"),
}

/** 저장 엔티티와 1:1 인 도메인 카드 (FSRS 상태 포함) */
data class Card(
    val id: Long,
    val wordId: String,
    val senseId: String?,
    val skill: Skill,
    val state: CardState,
    val due: Long,
    val stability: Double,
    val difficulty: Double,
    val reps: Int,
    val lapses: Int,
    val lastReview: Long?,
    val createdAt: Long,
    val step: Int = 0,
) {
    fun toFsrs() = FsrsCard(state, due, stability, difficulty, reps, lapses, lastReview, step)
}

/** 0 새 단어 · 1 만남 · 2 학습 중 · 3 익숙 · 4 숙달 */
enum class Mastery(val level: Int, val ko: String) {
    NEW(0, "새 단어"), MET(1, "만남"), LEARNING(2, "학습 중"), FAMILIAR(3, "익숙"), MASTERED(4, "숙달");

    companion object {
        fun of(level: Int) = entries.firstOrNull { it.level == level } ?: NEW
    }
}

data class WordStatus(
    val wordId: String,
    val mastery: Mastery,
    val introducedAt: Long?,
    val masteredAt: Long?,
    val isBookmarked: Boolean,
    val note: String?,
)

data class DailyStat(val date: String, val newLearned: Int, val reviewed: Int, val correct: Int, val seconds: Int)
