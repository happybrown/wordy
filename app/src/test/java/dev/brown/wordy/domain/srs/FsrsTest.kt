package dev.brown.wordy.domain.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FsrsTest {
    private val fsrs = Fsrs()
    private val t0 = 1_700_000_000_000L
    private val day = Fsrs.DAY_MS.toLong()
    private fun newCard() = FsrsCard(due = t0)

    @Test fun `initial stability equals w0 to w3 and difficulty is clamped`() {
        assertEquals(0.212, fsrs.initialStability(Rating.AGAIN), 1e-9)
        assertEquals(8.2956, fsrs.initialStability(Rating.EASY), 1e-9)
        for (r in Rating.entries) assertTrue(fsrs.initialDifficulty(r) in 1.0..10.0)
        assertTrue(fsrs.initialDifficulty(Rating.AGAIN) > fsrs.initialDifficulty(Rating.EASY))
    }

    @Test fun `retrievability is 1 at t=0 and 0_9 after one stability period`() {
        assertEquals(1.0, fsrs.retrievability(0.0, 10.0), 1e-9)
        assertEquals(0.9, fsrs.retrievability(10.0, 10.0), 1e-9)
        assertTrue(fsrs.retrievability(30.0, 10.0) < 0.9)
    }

    @Test fun `next interval at retention 0_9 equals stability`() {
        assertEquals(10, fsrs.nextInterval(10.0))
        assertEquals(1, fsrs.nextInterval(0.2))
        assertTrue(Fsrs(desiredRetention = 0.8).nextInterval(10.0) > 10)
    }

    @Test fun `new card goes through learning steps`() {
        val again = fsrs.review(newCard(), Rating.AGAIN, t0)
        assertEquals(CardState.LEARNING, again.card.state)
        assertEquals(1.0 / 1440, again.scheduledDays, 1e-9) // 1 minute

        val good = fsrs.review(newCard(), Rating.GOOD, t0)
        assertEquals(CardState.LEARNING, good.card.state)
        assertEquals(1, good.card.step)
        assertEquals(10.0 / 1440, good.scheduledDays, 1e-9) // 10 minutes

        val good2 = fsrs.review(good.card, Rating.GOOD, t0 + 10 * 60_000)
        assertEquals(CardState.REVIEW, good2.card.state)
        assertTrue(good2.scheduledDays >= 1.0)

        val easy = fsrs.review(newCard(), Rating.EASY, t0)
        assertEquals(CardState.REVIEW, easy.card.state)
        assertTrue(easy.scheduledDays >= 8.0)
    }

    @Test fun `review intervals are ordered hard lt good lt easy and again lapses`() {
        var card = fsrs.review(newCard(), Rating.EASY, t0).card
        val later = card.due + day
        val preview = fsrs.preview(card, later)
        assertTrue(preview[Rating.HARD]!! <= preview[Rating.GOOD]!!)
        assertTrue(preview[Rating.GOOD]!! <= preview[Rating.EASY]!!)
        val again = fsrs.review(card, Rating.AGAIN, later)
        assertEquals(CardState.RELEARNING, again.card.state)
        assertEquals(1, again.card.lapses)
        assertTrue(again.card.stability < card.stability)
        val recovered = fsrs.review(again.card, Rating.GOOD, later + 10 * 60_000)
        assertEquals(CardState.REVIEW, recovered.card.state)
    }

    @Test fun `stability grows with successful spaced reviews`() {
        var card = fsrs.review(newCard(), Rating.GOOD, t0).card
        card = fsrs.review(card, Rating.GOOD, t0 + 10 * 60_000).card
        var previous = card.stability
        var now = card.due
        var lastScheduled = 0.0
        repeat(5) {
            val result = fsrs.review(card, Rating.GOOD, now)
            card = result.card
            assertTrue("stability should grow: ${card.stability} > $previous", card.stability > previous)
            assertTrue("interval should grow: ${result.scheduledDays} > $lastScheduled", result.scheduledDays > lastScheduled)
            previous = card.stability
            lastScheduled = result.scheduledDays
            now = card.due
        }
        assertEquals(7, card.reps)
        assertTrue(lastScheduled >= 1.0)
    }

    @Test fun `difficulty stays within bounds under extreme sequences`() {
        var card = fsrs.review(newCard(), Rating.AGAIN, t0).card
        var now = t0
        repeat(30) { now += day; card = fsrs.review(card, Rating.AGAIN, now).card }
        assertTrue(card.difficulty <= 10.0)
        repeat(30) { now += day; card = fsrs.review(card, Rating.EASY, now).card }
        assertTrue(card.difficulty >= 1.0)
    }
}
