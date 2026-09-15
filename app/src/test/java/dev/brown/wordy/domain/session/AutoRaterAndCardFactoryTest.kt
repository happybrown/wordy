package dev.brown.wordy.domain.session

import dev.brown.wordy.domain.model.Example
import dev.brown.wordy.domain.model.Level
import dev.brown.wordy.domain.model.Pos
import dev.brown.wordy.domain.model.Sense
import dev.brown.wordy.domain.model.Skill
import dev.brown.wordy.domain.model.Word
import dev.brown.wordy.domain.srs.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoRaterAndCardFactoryTest {
    private fun sense(id: String, colloc: List<String> = emptyList()) = Sense(id, "get", 0, Pos.V, "받다", "to receive", null, null, listOf(Example("I got it.", "받았어.", "got")), colloc, emptyList())
    private fun word(core: Boolean, senses: Int) = Word("get", "get", null, Level.PRE, "A1", 43, core, "abstract_general", "u", listOf("got"), emptyList(), emptyList(), null, (1..senses).map { sense("get-$it", if (core) listOf("get a job", "get home") else emptyList()) })

    @Test fun `rating follows correctness, hint and speed`() {
        assertEquals(Rating.AGAIN, AutoRater.rate(AnswerOutcome(false, elapsedMs = 1000), ExerciseType.MEANING_CHOICE))
        assertEquals(Rating.HARD, AutoRater.rate(AnswerOutcome(false, almost = true, elapsedMs = 1000), ExerciseType.RECALL_TYPE))
        assertEquals(Rating.HARD, AutoRater.rate(AnswerOutcome(true, usedHint = true, elapsedMs = 1000), ExerciseType.RECALL_TYPE))
        assertEquals(Rating.EASY, AutoRater.rate(AnswerOutcome(true, elapsedMs = 2000), ExerciseType.MEANING_CHOICE))
        assertEquals(Rating.GOOD, AutoRater.rate(AnswerOutcome(true, elapsedMs = 5000), ExerciseType.MEANING_CHOICE))
        assertEquals(Rating.EASY, AutoRater.rate(AnswerOutcome(true, elapsedMs = 5000), ExerciseType.RECALL_TYPE))
    }

    @Test fun `light word gets recognize and context per sense plus recall`() {
        val cards = CardFactory.cardsFor(word(core = false, senses = 2), now = 0)
        assertEquals(5, cards.size)
        assertEquals(2, cards.count { it.skill == Skill.RECOGNIZE })
        assertEquals(2, cards.count { it.skill == Skill.CONTEXT })
        assertEquals(1, cards.count { it.skill == Skill.RECALL })
        assertEquals(0, cards.count { it.skill == Skill.USE })
        assertEquals(0L, cards.first { it.skill == Skill.RECOGNIZE && it.senseId == "get-1" }.due)
        assertTrue(cards.first { it.skill == Skill.RECOGNIZE && it.senseId == "get-2" }.due >= 86_400_000L)
    }

    @Test fun `core word with collocations also gets a USE card`() {
        val cards = CardFactory.cardsFor(word(core = true, senses = 4), now = 0)
        assertEquals(4 * 2 + 2, cards.size)
        assertEquals(1, cards.count { it.skill == Skill.USE })
    }
}
