package dev.brown.wordy.data.db.progress

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** FSRS 카드. skill: RECOGNIZE/CONTEXT 는 sense 단위(senseId != null), RECALL/LISTEN/USE 는 단어 단위(senseId == null). */
@Entity(
    tableName = "card",
    indices = [Index("due"), Index("wordId"), Index(value = ["wordId", "senseId", "skill"], unique = true)],
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: String,
    val senseId: String?,
    val skill: String,
    /** NEW / LEARNING / REVIEW / RELEARNING */
    val state: String,
    /** epoch millis */
    val due: Long,
    val stability: Double,
    val difficulty: Double,
    val reps: Int,
    val lapses: Int,
    val lastReview: Long?,
    val createdAt: Long,
    /** FSRS 학습/재학습 단계 인덱스 */
    val step: Int = 0,
)

@Entity(tableName = "review_log", indices = [Index("cardId"), Index("reviewedAt"), Index("wordId")])
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val wordId: String,
    val reviewedAt: Long,
    /** 1 Again, 2 Hard, 3 Good, 4 Easy */
    val rating: Int,
    val exerciseType: String,
    val wasCorrect: Boolean,
    val elapsedMs: Long,
    val scheduledDays: Double,
)

@Entity(tableName = "word_status", indices = [Index("mastery"), Index("isBookmarked")])
data class WordStatusEntity(
    @PrimaryKey val wordId: String,
    /** 0 새 단어, 1 만남, 2 학습 중, 3 익숙, 4 숙달 */
    val mastery: Int,
    val introducedAt: Long?,
    val masteredAt: Long?,
    val isBookmarked: Boolean = false,
    val note: String? = null,
    val updatedAt: Long,
)

@Entity(tableName = "daily_stat")
data class DailyStatEntity(
    /** yyyy-MM-dd (기기 로컬 날짜) */
    @PrimaryKey val date: String,
    val newLearned: Int,
    val reviewed: Int,
    val correct: Int,
    val seconds: Int,
)
