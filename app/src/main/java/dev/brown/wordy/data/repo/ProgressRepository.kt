package dev.brown.wordy.data.repo

import dev.brown.wordy.data.db.progress.CardEntity
import dev.brown.wordy.data.db.progress.DailyStatEntity
import dev.brown.wordy.data.db.progress.ProgressDatabase
import dev.brown.wordy.data.db.progress.ReviewLogEntity
import dev.brown.wordy.data.db.progress.WordStatusEntity
import dev.brown.wordy.domain.model.Card
import dev.brown.wordy.domain.model.DailyStat
import dev.brown.wordy.domain.model.Mastery
import dev.brown.wordy.domain.model.Skill
import dev.brown.wordy.domain.model.WordStatus
import dev.brown.wordy.domain.srs.CardState
import dev.brown.wordy.domain.srs.FsrsCard
import dev.brown.wordy.domain.srs.Rating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

/** progress.db 접근. 도메인 ↔ 엔티티 변환만 담당하고 학습 규칙은 domain 에 둔다. */
class ProgressRepository(private val db: ProgressDatabase) {
    private val cards = db.cards()
    private val logs = db.reviewLogs()
    private val statuses = db.wordStatus()
    private val daily = db.dailyStats()

    suspend fun cardsForWord(wordId: String): List<Card> = cards.byWord(wordId).map { it.toDomain() }
    suspend fun cardsForWords(wordIds: List<String>): List<Card> = wordIds.chunked(500).flatMap { cards.byWords(it) }.map { it.toDomain() }
    suspend fun dueCards(now: Long, limit: Int): List<Card> = cards.due(now, limit).map { it.toDomain() }
    fun dueCount(now: Long): Flow<Int> = cards.dueCount(now)
    suspend fun dueBetween(from: Long, until: Long): Int = cards.dueBetween(from, until)
    suspend fun introducedWordIds(): Set<String> = cards.introducedWordIds().toSet()

    suspend fun insertCards(newCards: List<Card>) { cards.insertAll(newCards.map { it.toEntity() }) }

    suspend fun applyReview(card: Card, updated: FsrsCard, rating: Rating, exerciseType: String, wasCorrect: Boolean, elapsedMs: Long, scheduledDays: Double, now: Long): Card {
        val next = card.copy(
            state = updated.state, due = updated.due, stability = updated.stability, difficulty = updated.difficulty,
            reps = updated.reps, lapses = updated.lapses, lastReview = updated.lastReview, step = updated.step,
        )
        cards.update(next.toEntity())
        logs.insert(ReviewLogEntity(cardId = card.id, wordId = card.wordId, reviewedAt = now, rating = rating.value, exerciseType = exerciseType, wasCorrect = wasCorrect, elapsedMs = elapsedMs, scheduledDays = scheduledDays))
        return next
    }

    suspend fun status(wordId: String): WordStatus? = statuses.byWord(wordId)?.toDomain()
    fun observeStatus(wordId: String): Flow<WordStatus?> = statuses.observe(wordId).map { it?.toDomain() }
    suspend fun statuses(wordIds: List<String>): Map<String, WordStatus> = wordIds.chunked(500).flatMap { statuses.byWords(it) }.associate { it.wordId to it.toDomain() }
    fun observeAllStatuses(): Flow<List<WordStatus>> = statuses.observeAll().map { list -> list.map { it.toDomain() } }
    fun bookmarked(): Flow<List<WordStatus>> = statuses.bookmarked().map { list -> list.map { it.toDomain() } }

    suspend fun upsertStatus(status: WordStatus, now: Long) {
        statuses.upsert(WordStatusEntity(status.wordId, status.mastery.level, status.introducedAt, status.masteredAt, status.isBookmarked, status.note, now))
    }

    suspend fun setBookmark(wordId: String, bookmarked: Boolean, now: Long) {
        val cur = status(wordId) ?: WordStatus(wordId, Mastery.NEW, null, null, false, null)
        upsertStatus(cur.copy(isBookmarked = bookmarked), now)
    }

    suspend fun setNote(wordId: String, note: String?, now: Long) {
        val cur = status(wordId) ?: WordStatus(wordId, Mastery.NEW, null, null, false, null)
        upsertStatus(cur.copy(note = note?.takeIf { it.isNotBlank() }), now)
    }

    suspend fun recentLogs(wordId: String, limit: Int = 20) = logs.byWord(wordId, limit)
    suspend fun logsSince(since: Long) = logs.since(since)
    suspend fun totalReviews() = logs.count()

    fun observeDaily(date: String): Flow<DailyStat?> = daily.observe(date).map { it?.toDomain() }
    fun recentDaily(limit: Int): Flow<List<DailyStat>> = daily.recent(limit).map { list -> list.map { it.toDomain() } }
    suspend fun allDaily(): List<DailyStat> = daily.all().map { it.toDomain() }

    suspend fun bumpDaily(date: String, newLearned: Int = 0, reviewed: Int = 0, correct: Int = 0, seconds: Int = 0) {
        val cur = daily.byDate(date) ?: DailyStatEntity(date, 0, 0, 0, 0)
        daily.upsert(cur.copy(newLearned = cur.newLearned + newLearned, reviewed = cur.reviewed + reviewed, correct = cur.correct + correct, seconds = cur.seconds + seconds))
    }

    companion object {
        fun today(now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String =
            java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toString()

        fun dateOf(offsetDays: Long, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String =
            java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(offsetDays).toString()

        fun startOfDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}

private fun CardEntity.toDomain() = Card(id, wordId, senseId, Skill.valueOf(skill), CardState.valueOf(state), due, stability, difficulty, reps, lapses, lastReview, createdAt, step)
private fun Card.toEntity() = CardEntity(id, wordId, senseId, skill.name, state.name, due, stability, difficulty, reps, lapses, lastReview, createdAt, step)
private fun WordStatusEntity.toDomain() = WordStatus(wordId, Mastery.of(mastery), introducedAt, masteredAt, isBookmarked, note)
private fun DailyStatEntity.toDomain() = DailyStat(date, newLearned, reviewed, correct, seconds)
