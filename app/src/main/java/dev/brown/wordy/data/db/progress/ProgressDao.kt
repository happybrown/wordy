package dev.brown.wordy.data.db.progress

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cards: List<CardEntity>): List<Long>

    @Update
    suspend fun update(card: CardEntity)

    @Query("SELECT * FROM card WHERE id = :id")
    suspend fun byId(id: Long): CardEntity?

    @Query("SELECT * FROM card WHERE wordId = :wordId")
    suspend fun byWord(wordId: String): List<CardEntity>

    @Query("SELECT * FROM card WHERE wordId IN (:wordIds)")
    suspend fun byWords(wordIds: List<String>): List<CardEntity>

    /**
     * 복습 큐. state 로 거르지 않는다: CardFactory 가 도입 시 만든 예약 카드(2번째 sense, RECALL, USE)는
     * NEW 상태로 미래 due 를 갖고, 그 날짜가 되면 첫 노출과 함께 복습 큐에 들어와야 한다.
     * 아직 도입하지 않은 단어는 카드 자체가 없으므로 여기 걸리지 않는다.
     */
    @Query("SELECT * FROM card WHERE due <= :now ORDER BY due LIMIT :limit")
    suspend fun due(now: Long, limit: Int): List<CardEntity>

    @Query("SELECT COUNT(*) FROM card WHERE due <= :now")
    fun dueCount(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM card WHERE due > :now AND due <= :until")
    suspend fun dueBetween(now: Long, until: Long): Int

    @Query("SELECT DISTINCT wordId FROM card")
    suspend fun introducedWordIds(): List<String>

    @Query("SELECT * FROM card")
    suspend fun all(): List<CardEntity>

    /**
     * 복원 전용. insertAll 은 IGNORE 라 (wordId, senseId, skill) 유니크 제약에 걸리면 조용히 버린다.
     * 그 동작은 단어를 두 번 도입해도 카드가 안 늘어나게 하려는 것이라 복원에는 맞지 않는다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllForRestore(cards: List<CardEntity>)

    @Query("DELETE FROM card")
    suspend fun deleteAll()
}

@Dao
interface ReviewLogDao {
    @Insert
    suspend fun insert(log: ReviewLogEntity): Long

    @Query("SELECT * FROM review_log WHERE wordId = :wordId ORDER BY reviewedAt DESC LIMIT :limit")
    suspend fun byWord(wordId: String, limit: Int): List<ReviewLogEntity>

    @Query("SELECT * FROM review_log WHERE reviewedAt >= :since ORDER BY reviewedAt")
    suspend fun since(since: Long): List<ReviewLogEntity>

    @Query("SELECT COUNT(*) FROM review_log")
    suspend fun count(): Int

    @Query("SELECT * FROM review_log")
    suspend fun all(): List<ReviewLogEntity>

    @Insert
    suspend fun insertAll(logs: List<ReviewLogEntity>)

    @Query("DELETE FROM review_log")
    suspend fun deleteAll()
}

@Dao
interface WordStatusDao {
    @Upsert
    suspend fun upsert(status: WordStatusEntity)

    @Upsert
    suspend fun upsertAll(statuses: List<WordStatusEntity>)

    @Query("SELECT * FROM word_status WHERE wordId = :wordId")
    suspend fun byWord(wordId: String): WordStatusEntity?

    @Query("SELECT * FROM word_status WHERE wordId = :wordId")
    fun observe(wordId: String): Flow<WordStatusEntity?>

    @Query("SELECT * FROM word_status WHERE wordId IN (:wordIds)")
    suspend fun byWords(wordIds: List<String>): List<WordStatusEntity>

    @Query("SELECT * FROM word_status")
    fun observeAll(): Flow<List<WordStatusEntity>>

    @Query("SELECT * FROM word_status WHERE isBookmarked = 1 ORDER BY updatedAt DESC")
    fun bookmarked(): Flow<List<WordStatusEntity>>

    @Query("SELECT wordId FROM word_status")
    suspend fun knownWordIds(): List<String>

    @Query("SELECT * FROM word_status")
    suspend fun all(): List<WordStatusEntity>

    @Query("DELETE FROM word_status")
    suspend fun deleteAll()
}

@Dao
interface DailyStatDao {
    @Upsert
    suspend fun upsert(stat: DailyStatEntity)

    @Query("SELECT * FROM daily_stat WHERE date = :date")
    suspend fun byDate(date: String): DailyStatEntity?

    @Query("SELECT * FROM daily_stat WHERE date = :date")
    fun observe(date: String): Flow<DailyStatEntity?>

    @Query("SELECT * FROM daily_stat ORDER BY date DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<DailyStatEntity>>

    @Query("SELECT * FROM daily_stat ORDER BY date DESC")
    suspend fun all(): List<DailyStatEntity>

    @Upsert
    suspend fun upsertAll(stats: List<DailyStatEntity>)

    @Query("DELETE FROM daily_stat")
    suspend fun deleteAll()
}
