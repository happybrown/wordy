package dev.brown.wordy.platform

import dev.brown.wordy.data.db.progress.CardEntity
import dev.brown.wordy.data.db.progress.DailyStatEntity
import dev.brown.wordy.data.db.progress.ProgressDatabase
import dev.brown.wordy.data.db.progress.ReviewLogEntity
import dev.brown.wordy.data.db.progress.WordStatusEntity
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * 학습 기록 백업·복원.
 *
 * content.db 는 앱에 내장되어 있어 백업할 필요가 없다. progress.db 만 내보낸다.
 * 형식은 SQLite 파일이 아니라 JSON 이다. 스키마가 바뀌어도 읽을 수 있고, 사용자가
 * 내용을 직접 확인할 수 있기 때문이다.
 */
class BackupManager(private val db: ProgressDatabase) {

    @Serializable
    data class Backup(
        /**
         * 백업 형식 버전. 스키마가 바뀌면 올리고 [peek] 에서 거른다.
         * [EncodeDefault] 가 없으면 기본값과 같을 때 필드가 아예 빠져 나가고,
         * 그러면 구버전 앱이 미래 형식 파일을 그냥 받아들여 버린다.
         */
        @EncodeDefault(EncodeDefault.Mode.ALWAYS)
        val formatVersion: Int = FORMAT_VERSION,
        val exportedAt: Long,
        val appVersion: String,
        val contentVersion: String,
        val cards: List<CardRow>,
        val reviewLogs: List<ReviewLogRow>,
        val wordStatus: List<WordStatusRow>,
        val dailyStats: List<DailyStatRow>,
    )

    // 엔티티를 그대로 직렬화하지 않고 별도 DTO 를 둔다. Room 엔티티에 컬럼을 추가해도
    // 백업 파일 형식이 조용히 따라 바뀌지 않게 하기 위해서다.
    @Serializable
    data class CardRow(
        val wordId: String, val senseId: String?, val skill: String, val state: String,
        val due: Long, val stability: Double, val difficulty: Double,
        val reps: Int, val lapses: Int, val lastReview: Long?, val createdAt: Long, val step: Int,
    )

    @Serializable
    data class ReviewLogRow(
        val wordId: String, val reviewedAt: Long, val rating: Int,
        val exerciseType: String, val wasCorrect: Boolean, val elapsedMs: Long, val scheduledDays: Double,
        /** 내보낼 때의 카드 id. 복원 시 새 id 로 다시 매핑한다. */
        val cardKey: String,
    )

    @Serializable
    data class WordStatusRow(
        val wordId: String, val mastery: Int, val introducedAt: Long?, val masteredAt: Long?,
        val isBookmarked: Boolean, val note: String?, val updatedAt: Long,
    )

    @Serializable
    data class DailyStatRow(
        val date: String, val newLearned: Int, val reviewed: Int, val correct: Int, val seconds: Int,
    )

    data class Summary(val cards: Int, val reviews: Int, val words: Int, val days: Int)

    suspend fun export(out: OutputStream, appVersion: String, contentVersion: String, now: Long): Summary {
        val cards = db.cards().all()
        val logs = db.reviewLogs().all()
        val statuses = db.wordStatus().all()
        val stats = db.dailyStats().all()

        // 카드 id 는 autoGenerate 라 복원하면 달라진다. 리뷰 로그가 어느 카드의
        // 것인지는 (wordId, senseId, skill) 조합으로 다시 찾는다.
        val keyById = cards.associate { it.id to cardKey(it.wordId, it.senseId, it.skill) }

        val backup = Backup(
            exportedAt = now,
            appVersion = appVersion,
            contentVersion = contentVersion,
            cards = cards.map {
                CardRow(it.wordId, it.senseId, it.skill, it.state, it.due, it.stability, it.difficulty,
                    it.reps, it.lapses, it.lastReview, it.createdAt, it.step)
            },
            reviewLogs = logs.mapNotNull { log ->
                val key = keyById[log.cardId] ?: return@mapNotNull null
                ReviewLogRow(log.wordId, log.reviewedAt, log.rating, log.exerciseType,
                    log.wasCorrect, log.elapsedMs, log.scheduledDays, key)
            },
            wordStatus = statuses.map {
                WordStatusRow(it.wordId, it.mastery, it.introducedAt, it.masteredAt, it.isBookmarked, it.note, it.updatedAt)
            },
            dailyStats = stats.map { DailyStatRow(it.date, it.newLearned, it.reviewed, it.correct, it.seconds) },
        )
        out.bufferedWriter().use { it.write(json.encodeToString(backup)) }
        return Summary(backup.cards.size, backup.reviewLogs.size, backup.wordStatus.size, backup.dailyStats.size)
    }

    /** 파일을 읽어 검증만 한다. 복원 전에 사용자에게 내용을 보여주기 위한 것. */
    fun peek(input: InputStream): Result<Backup> = runCatching {
        val backup = json.decodeFromString<Backup>(input.bufferedReader().use { it.readText() })
        require(backup.formatVersion <= FORMAT_VERSION) {
            "이 백업 파일은 더 새로운 버전(${backup.formatVersion})입니다. 앱을 업데이트해 주세요."
        }
        backup
    }

    /**
     * 복원. 기존 학습 기록을 **모두 지우고** 백업 내용으로 바꾼다.
     * 병합하지 않는 이유는 같은 단어의 FSRS 상태가 둘일 때 어느 쪽이 맞는지 정할 근거가 없어서다.
     */
    suspend fun restore(backup: Backup): Summary {
        db.cards().deleteAll()
        db.reviewLogs().deleteAll()
        db.wordStatus().deleteAll()
        db.dailyStats().deleteAll()

        db.cards().insertAllForRestore(
            backup.cards.map {
                CardEntity(0, it.wordId, it.senseId, it.skill, it.state, it.due, it.stability,
                    it.difficulty, it.reps, it.lapses, it.lastReview, it.createdAt, it.step)
            }
        )
        // 새로 발급된 id 로 리뷰 로그를 다시 연결한다.
        val idByKey = db.cards().all().associate { cardKey(it.wordId, it.senseId, it.skill) to it.id }
        db.reviewLogs().insertAll(
            backup.reviewLogs.mapNotNull { row ->
                val cardId = idByKey[row.cardKey] ?: return@mapNotNull null
                ReviewLogEntity(0, cardId, row.wordId, row.reviewedAt, row.rating,
                    row.exerciseType, row.wasCorrect, row.elapsedMs, row.scheduledDays)
            }
        )
        db.wordStatus().upsertAll(
            backup.wordStatus.map {
                WordStatusEntity(it.wordId, it.mastery, it.introducedAt, it.masteredAt, it.isBookmarked, it.note, it.updatedAt)
            }
        )
        db.dailyStats().upsertAll(
            backup.dailyStats.map { DailyStatEntity(it.date, it.newLearned, it.reviewed, it.correct, it.seconds) }
        )
        return Summary(backup.cards.size, backup.reviewLogs.size, backup.wordStatus.size, backup.dailyStats.size)
    }

    private fun cardKey(wordId: String, senseId: String?, skill: String) = "$wordId|${senseId ?: ""}|$skill"

    companion object {
        const val FORMAT_VERSION = 1
        private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

        fun fileName(now: Long): String {
            val d = java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            return "wordy-backup-%04d%02d%02d.json".format(d.year, d.monthValue, d.dayOfMonth)
        }
    }
}
