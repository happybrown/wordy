package dev.brown.wordy.domain.mastery

import dev.brown.wordy.domain.model.Card
import dev.brown.wordy.domain.model.Mastery
import dev.brown.wordy.domain.model.Skill
import dev.brown.wordy.domain.srs.CardState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MasteryCalculatorTest {
    private fun card(state: CardState, stability: Double, reps: Int = 1) = Card(0, "w", null, Skill.RECALL, state, 0, stability, 5.0, reps, 0, null, 0)

    @Test fun `mastery ladder`() {
        assertEquals(Mastery.NEW, MasteryCalculator.compute(emptyList()))
        assertEquals(Mastery.MET, MasteryCalculator.compute(listOf(card(CardState.NEW, 0.0, reps = 0))))
        assertEquals(Mastery.LEARNING, MasteryCalculator.compute(listOf(card(CardState.LEARNING, 1.0), card(CardState.REVIEW, 40.0))))
        assertEquals(Mastery.LEARNING, MasteryCalculator.compute(listOf(card(CardState.REVIEW, 3.0))))
        assertEquals(Mastery.FAMILIAR, MasteryCalculator.compute(listOf(card(CardState.REVIEW, 8.0), card(CardState.REVIEW, 50.0))))
        assertEquals(Mastery.MASTERED, MasteryCalculator.compute(listOf(card(CardState.REVIEW, 31.0), card(CardState.REVIEW, 50.0))))
    }

    @Test fun `progress is monotonic in state and stability`() {
        val a = MasteryCalculator.progress(listOf(card(CardState.NEW, 0.0, 0)))
        val b = MasteryCalculator.progress(listOf(card(CardState.LEARNING, 1.0)))
        val c = MasteryCalculator.progress(listOf(card(CardState.REVIEW, 10.0)))
        val d = MasteryCalculator.progress(listOf(card(CardState.REVIEW, 60.0)))
        assertTrue(a < b && b < c && c < d && d <= 1f)
    }
}
