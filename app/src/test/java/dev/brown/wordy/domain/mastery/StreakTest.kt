package dev.brown.wordy.domain.mastery

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * 연속 학습일 계산. 오늘 아직 학습하지 않았어도 어제까지 이어졌으면 스트릭은 유지된다.
 * 하루라도 비면 거기서 끊긴다.
 */
class StreakTest {
    private val today = LocalDate.of(2026, 9, 16)

    private fun days(vararg back: Long) = back.map { today.minusDays(it).toString() }.toSet()

    @Test
    fun `오늘 포함 연속 3일`() {
        assertEquals(3, streakOf(days(0, 1, 2), today))
    }

    @Test
    fun `오늘 아직 안 했어도 어제까지 이어졌으면 유지된다`() {
        assertEquals(2, streakOf(days(1, 2), today))
    }

    @Test
    fun `하루 비면 끊긴다`() {
        // 오늘, 어제 학습했고 그제는 건너뛰었다
        assertEquals(2, streakOf(days(0, 1, 3, 4), today))
    }

    @Test
    fun `이틀 전까지만 있으면 0`() {
        assertEquals(0, streakOf(days(2, 3), today))
    }

    @Test
    fun `기록이 없으면 0`() {
        assertEquals(0, streakOf(emptySet(), today))
    }

    @Test
    fun `오늘만 했으면 1`() {
        assertEquals(1, streakOf(days(0), today))
    }
}
