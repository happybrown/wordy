package dev.brown.wordy.domain.placement

import dev.brown.wordy.domain.model.Level
import org.junit.Assert.assertEquals
import org.junit.Test

class PlacementTest {
    private fun asked(n: Int) = Level.entries.associateWith { n }

    @Test
    fun `다 틀리면 가장 낮은 레벨`() {
        val correct = Level.entries.associateWith { 0 }
        assertEquals(Level.PRE, Placement.recommend(correct, asked(6)))
    }

    @Test
    fun `초등까지 통과하면 중등을 추천한다`() {
        // 통과한 가장 높은 레벨의 '다음'을 준다. 이미 아는 레벨을 다시 훑지 않게.
        val correct = mapOf(Level.PRE to 6, Level.ELEM to 5, Level.MID to 2, Level.HIGH to 1, Level.ADV to 0)
        assertEquals(Level.MID, Placement.recommend(correct, asked(6)))
    }

    @Test
    fun `모든 레벨을 통과하면 최고 레벨에 머문다`() {
        val correct = Level.entries.associateWith { 6 }
        assertEquals(Level.ADV, Placement.recommend(correct, asked(6)))
    }

    @Test
    fun `정확히 70퍼센트면 통과다`() {
        // 10문제 중 7개 = 0.7, 경계값이 통과 쪽에 있어야 한다
        val correct = mapOf(Level.PRE to 7, Level.ELEM to 6)
        val askedMap = mapOf(Level.PRE to 10, Level.ELEM to 10)
        assertEquals(Level.ELEM, Placement.recommend(correct, askedMap))
    }

    @Test
    fun `낮은 레벨을 틀려도 높은 레벨을 통과하면 그쪽을 따른다`() {
        // 찍었거나 특정 분야만 아는 경우. 연속성을 요구하지 않는다.
        val correct = mapOf(Level.PRE to 2, Level.ELEM to 6, Level.MID to 0)
        assertEquals(Level.MID, Placement.recommend(correct, asked(6)))
    }

    @Test
    fun `출제하지 않은 레벨은 판단에서 제외한다`() {
        // ADV 를 0문제 냈는데 correct 가 0이라고 "통과 못 함"으로 세면 안 된다
        val correct = mapOf(Level.PRE to 6, Level.ELEM to 6, Level.MID to 6, Level.HIGH to 6)
        val askedMap = mapOf(Level.PRE to 6, Level.ELEM to 6, Level.MID to 6, Level.HIGH to 6, Level.ADV to 0)
        assertEquals(Level.ADV, Placement.recommend(correct, askedMap))
    }

    @Test
    fun `아무것도 출제하지 않으면 가장 낮은 레벨`() {
        assertEquals(Level.PRE, Placement.recommend(emptyMap(), emptyMap()))
    }

    @Test
    fun `요약 문구에 레벨별 점수가 들어간다`() {
        val correct = mapOf(Level.PRE to 6, Level.ELEM to 4)
        val askedMap = mapOf(Level.PRE to 6, Level.ELEM to 6)
        val s = Placement.summary(correct, askedMap, Level.ELEM)
        assertEquals("12문제 중 10개 정답 (입문 6/6 · 초등 4/6) → 초등 레벨을 추천합니다.", s)
    }
}
