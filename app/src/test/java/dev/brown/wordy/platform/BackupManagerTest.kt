package dev.brown.wordy.platform

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.brown.wordy.data.db.progress.CardEntity
import dev.brown.wordy.data.db.progress.DailyStatEntity
import dev.brown.wordy.data.db.progress.ProgressDatabase
import dev.brown.wordy.data.db.progress.ReviewLogEntity
import dev.brown.wordy.data.db.progress.WordStatusEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * 백업 왕복 검증. 특히 카드 id 재매핑이 핵심이다.
 * 카드 id 는 autoGenerate 라 복원하면 값이 달라지므로, 리뷰 로그는 id 가 아니라
 * (wordId, senseId, skill) 조합으로 다시 연결해야 한다.
 */
@RunWith(RobolectricTestRunner::class)
class BackupManagerTest {
    private lateinit var db: ProgressDatabase
    private lateinit var backup: BackupManager

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<ProgressDatabase>(ctx)
            .setDriver(BundledSQLiteDriver())
            .build()
        backup = BackupManager(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun seed() {
        db.cards().insertAll(
            listOf(
                CardEntity(0, "get", "get-1", "RECOGNIZE", "REVIEW", 1_000L, 12.5, 5.1, 3, 1, 900L, 100L, 0),
                CardEntity(0, "get", null, "RECALL", "LEARNING", 2_000L, 1.0, 6.0, 1, 0, 1_900L, 100L, 1),
                CardEntity(0, "take", "take-2", "CONTEXT", "NEW", 3_000L, 0.0, 5.0, 0, 0, null, 100L, 0),
            )
        )
        val ids = db.cards().all().associate { "${it.wordId}|${it.senseId ?: ""}|${it.skill}" to it.id }
        db.reviewLogs().insertAll(
            listOf(
                ReviewLogEntity(0, ids.getValue("get|get-1|RECOGNIZE"), "get", 950L, 3, "MCQ", true, 2_400L, 6.0),
                ReviewLogEntity(0, ids.getValue("get||RECALL"), "get", 1_950L, 1, "TYPING", false, 8_100L, 0.0),
            )
        )
        db.wordStatus().upsertAll(
            listOf(
                WordStatusEntity("get", 3, 100L, null, true, "자주 틀림", 2_000L),
                WordStatusEntity("take", 1, 300L, null, false, null, 2_000L),
            )
        )
        db.dailyStats().upsertAll(listOf(DailyStatEntity("2026-09-16", 5, 12, 10, 420)))
    }

    private suspend fun roundTrip(): BackupManager.Backup {
        val out = ByteArrayOutputStream()
        backup.export(out, "1.0", "1.4054.abc", 5_000L)
        return backup.peek(ByteArrayInputStream(out.toByteArray())).getOrThrow()
    }

    @Test
    fun `내보내고 복원하면 모든 학습 기록이 그대로 돌아온다`() = runTest {
        seed()
        val parsed = roundTrip()
        assertEquals(3, parsed.cards.size)
        assertEquals(2, parsed.reviewLogs.size)

        // 복원 전에 기존 데이터를 지워 카드 id 가 새로 발급되게 한다
        val before = db.cards().all().map { it.id }.toSet()
        backup.restore(parsed)
        val after = db.cards().all()

        assertEquals(3, after.size)
        assertEquals(2, db.reviewLogs().count())
        assertEquals(2, db.wordStatus().all().size)
        assertEquals(1, db.dailyStats().all().size)

        // FSRS 상태가 보존되는지 — 이게 틀리면 복습 일정이 리셋된다
        val recognize = after.single { it.wordId == "get" && it.skill == "RECOGNIZE" }
        assertEquals(12.5, recognize.stability, 1e-9)
        assertEquals(5.1, recognize.difficulty, 1e-9)
        assertEquals(1_000L, recognize.due)
        assertEquals(3, recognize.reps)
        assertEquals(1, recognize.lapses)
        assertEquals("REVIEW", recognize.state)

        val status = db.wordStatus().byWord("get")!!
        assertEquals(3, status.mastery)
        assertTrue(status.isBookmarked)
        assertEquals("자주 틀림", status.note)
    }

    @Test
    fun `카드 id 가 바뀌어도 리뷰 로그가 올바른 카드에 붙는다`() = runTest {
        seed()
        val parsed = roundTrip()

        // 복원 전에 카드를 더 넣어 id 시퀀스를 밀어낸다. 그대로 id 를 쓰면 엉뚱한 카드에 붙는다.
        db.cards().insertAll(listOf(CardEntity(0, "filler", "filler-1", "RECOGNIZE", "NEW", 0L, 0.0, 5.0, 0, 0, null, 0L, 0)))
        backup.restore(parsed)

        val byKey = db.cards().all().associateBy { "${it.wordId}|${it.senseId ?: ""}|${it.skill}" }
        val recognizeId = byKey.getValue("get|get-1|RECOGNIZE").id
        val recallId = byKey.getValue("get||RECALL").id

        val logs = db.reviewLogs().all()
        val mcq = logs.single { it.exerciseType == "MCQ" }
        val typing = logs.single { it.exerciseType == "TYPING" }

        assertEquals(recognizeId, mcq.cardId)
        assertEquals(recallId, typing.cardId)
        assertNotEquals(mcq.cardId, typing.cardId)
        assertTrue(mcq.wasCorrect)
        assertEquals(false, typing.wasCorrect)
    }

    @Test
    fun `복원은 기존 기록을 병합하지 않고 교체한다`() = runTest {
        seed()
        val parsed = roundTrip()

        // 백업에 없는 단어를 넣어 두면 복원 후 사라져야 한다
        db.cards().deleteAll()
        db.cards().insertAll(listOf(CardEntity(0, "ghost", "ghost-1", "RECOGNIZE", "REVIEW", 77L, 9.0, 5.0, 2, 0, 70L, 10L, 0)))
        db.wordStatus().upsert(WordStatusEntity("ghost", 2, 10L, null, false, null, 10L))

        backup.restore(parsed)

        assertTrue(db.cards().all().none { it.wordId == "ghost" })
        assertEquals(null, db.wordStatus().byWord("ghost"))
        assertEquals(3, db.cards().all().size)
    }

    @Test
    fun `깨진 파일은 예외 대신 실패한 Result 로 돌아온다`() {
        val garbage = ByteArrayInputStream("{ not json".toByteArray())
        assertTrue(backup.peek(garbage).isFailure)
    }

    @Test
    fun `더 새로운 형식 버전은 거부한다`() = runTest {
        seed()
        val out = ByteArrayOutputStream()
        backup.export(out, "1.0", "1.4054.abc", 5_000L)
        val original = out.toString()
        val marker = "\"formatVersion\":${BackupManager.FORMAT_VERSION}"
        assertTrue("직렬화 형식이 바뀌었다: $original".take(200), original.contains(marker))
        val bumped = original.replace(marker, "\"formatVersion\":${BackupManager.FORMAT_VERSION + 1}")

        val result = backup.peek(ByteArrayInputStream(bumped.toByteArray()))
        assertTrue("더 새로운 형식을 받아들이면 안 된다", result.isFailure)
    }
}
