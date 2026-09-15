package dev.brown.wordy.data.db.content

import androidx.room3.Dao
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Query("SELECT * FROM word WHERE id = :id")
    suspend fun word(id: String): WordEntity?

    @Query("SELECT * FROM word WHERE id IN (:ids)")
    suspend fun words(ids: List<String>): List<WordEntity>

    @Query("SELECT * FROM word WHERE level = :level ORDER BY unitOrd, freqRank IS NULL, freqRank, headword")
    suspend fun wordsByLevel(level: String): List<WordEntity>

    @Query("SELECT * FROM word WHERE unitId = :unitId ORDER BY freqRank IS NULL, freqRank, headword")
    suspend fun wordsByUnit(unitId: String): List<WordEntity>

    @Query("SELECT * FROM word WHERE isCore = 1 ORDER BY freqRank IS NULL, freqRank, headword")
    suspend fun coreWords(): List<WordEntity>

    @Query("SELECT id FROM word WHERE level = :level ORDER BY unitOrd, freqRank IS NULL, freqRank, headword")
    suspend fun wordIdsByLevel(level: String): List<String>

    @Query("SELECT id FROM word WHERE isCore = 1 ORDER BY freqRank IS NULL, freqRank, headword")
    suspend fun coreWordIds(): List<String>

    @Query("SELECT * FROM sense WHERE wordId = :wordId ORDER BY ord")
    suspend fun senses(wordId: String): List<SenseEntity>

    @Query("SELECT * FROM sense WHERE wordId IN (:wordIds) ORDER BY wordId, ord")
    suspend fun sensesFor(wordIds: List<String>): List<SenseEntity>

    @Query("SELECT * FROM sense WHERE id = :id")
    suspend fun sense(id: String): SenseEntity?

    /** 오답지 후보: 같은 레벨·같은 품사의 다른 단어 sense (ord = 0 인 대표 의미만) */
    @Query(
        """SELECT s.* FROM sense s JOIN word w ON w.id = s.wordId
           WHERE w.level = :level AND s.pos = :pos AND s.ord = 0 AND w.id != :excludeWordId
           ORDER BY RANDOM() LIMIT :limit""",
    )
    suspend fun distractorSenses(level: String, pos: String, excludeWordId: String, limit: Int): List<SenseEntity>

    @Query(
        """SELECT s.* FROM sense s JOIN word w ON w.id = s.wordId
           WHERE w.isCore = 1 AND s.pos = :pos AND s.ord = 0 AND w.id != :excludeWordId
           ORDER BY RANDOM() LIMIT :limit""",
    )
    suspend fun coreDistractorSenses(pos: String, excludeWordId: String, limit: Int): List<SenseEntity>

    @Query(
        """SELECT s.* FROM sense s JOIN word w ON w.id = s.wordId
           WHERE w.level = :level AND s.ord = 0 AND w.id != :excludeWordId
           ORDER BY RANDOM() LIMIT :limit""",
    )
    suspend fun distractorSensesAnyPos(level: String, excludeWordId: String, limit: Int): List<SenseEntity>

    @Query("SELECT * FROM unit WHERE level = :level ORDER BY ord")
    suspend fun units(level: String): List<UnitEntity>

    @Query("SELECT * FROM unit ORDER BY level, ord")
    fun allUnits(): Flow<List<UnitEntity>>

    @Query("SELECT * FROM word WHERE searchText LIKE '%' || :query || '%' ORDER BY (headword = :query) DESC, (headword LIKE :query || '%') DESC, freqRank IS NULL, freqRank LIMIT :limit")
    suspend fun search(query: String, limit: Int): List<WordEntity>

    @Query("SELECT level, COUNT(*) AS count FROM word GROUP BY level")
    suspend fun countsByLevel(): List<LevelCount>

    @Query("SELECT value FROM content_meta WHERE key = :key")
    suspend fun meta(key: String): String?
}

data class LevelCount(val level: String, val count: Int)
