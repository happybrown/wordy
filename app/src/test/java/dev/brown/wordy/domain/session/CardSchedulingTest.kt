package dev.brown.wordy.domain.session

import dev.brown.wordy.domain.model.Example
import dev.brown.wordy.domain.model.Level
import dev.brown.wordy.domain.model.Pos
import dev.brown.wordy.domain.model.Sense
import dev.brown.wordy.domain.model.Skill
import dev.brown.wordy.domain.model.Word
import dev.brown.wordy.domain.srs.CardState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CardFactory 가 만든 예약 카드는 NEW 상태이면서 미래 due 를 갖는다.
 * 복습 큐가 state 로 거르면 이 카드들이 영원히 노출되지 않으므로, due 만으로 선택되어야 한다.
 */
class CardSchedulingTest {
    private val day = 86_400_000L

    private fun sense(id: String) = Sense(id, "w", 0, Pos.V, "뜻", "def", null, null, listOf(Example("I got it now.", "알겠어.", "got")), listOf("get a job", "get home"), emptyList())
    private fun word(core: Boolean, senses: Int) =
        Word("w", "w", null, Level.PRE, "A1", 1, core, "abstract_general", "u", emptyList(), emptyList(), emptyList(), null, (1..senses).map { sense("w-$it") })

    @Test fun `scheduled cards are NEW but dated in the future`() {
        val cards = CardFactory.cardsFor(word(core = true, senses = 3), now = 0)
        assertTrue(cards.all { it.state == CardState.NEW })
        val future = cards.filter { it.due > 0 }
        assertTrue("도입 이후로 예약된 카드가 있어야 한다", future.isNotEmpty())
        // 2번째 sense 이후의 인식/문맥 카드, RECALL, USE 는 모두 하루 이상 뒤
        assertTrue(future.any { it.skill == Skill.RECALL && it.due >= day })
        assertTrue(future.any { it.skill == Skill.USE && it.due >= day })
        assertTrue(future.any { it.skill == Skill.RECOGNIZE && it.due >= day })
    }

    @Test fun `a due filter based on state alone would hide scheduled cards`() {
        val cards = CardFactory.cardsFor(word(core = false, senses = 2), now = 0)
        val tomorrow = day + 1
        val dueByTime = cards.filter { it.due <= tomorrow }
        val dueByTimeAndState = dueByTime.filter { it.state != CardState.NEW }
        assertTrue("시간 기준으로는 내일 볼 카드가 있다", dueByTime.size > 1)
        assertEquals("state 기준으로 거르면 전부 사라진다 — 큐는 due 만 봐야 한다", 0, dueByTimeAndState.size)
    }

    @Test fun `first sense is introduced immediately and the rest are spread over days`() {
        val w = word(core = true, senses = 4)
        val cards = CardFactory.cardsFor(w, now = 0)
        val firstRecognize = cards.first { it.skill == Skill.RECOGNIZE && it.senseId == "w-1" }
        assertEquals(0L, firstRecognize.due)
        val laterRecognize = cards.filter { it.skill == Skill.RECOGNIZE && it.senseId != "w-1" }
        assertTrue(laterRecognize.all { it.due >= day })
        // sense 가 늘어날수록 뒤로 밀린다
        val dues = laterRecognize.map { it.due }.sorted()
        assertEquals(dues, dues.distinct())
    }
}
