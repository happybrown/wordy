package dev.brown.wordy.domain.session

import dev.brown.wordy.domain.model.Card
import dev.brown.wordy.domain.model.Skill
import dev.brown.wordy.domain.model.Word
import dev.brown.wordy.domain.srs.CardState

/**
 * 단어를 처음 만날 때 만드는 카드 묶음. due 를 조금씩 뒤로 두어 한 세션에 몰리지 않게 한다.
 * - sense 0: RECOGNIZE 지금, CONTEXT 5분 후
 * - 나머지 sense: RECOGNIZE 1일 후, CONTEXT 2일 후 (i 번째는 하루씩 추가)
 * - RECALL 1일 후, USE(Core 만) 3일 후
 */
object CardFactory {
    private const val MIN = 60_000L
    private const val DAY = 86_400_000L

    fun cardsFor(word: Word, now: Long): List<Card> {
        val cards = ArrayList<Card>()
        fun add(skill: Skill, senseId: String?, due: Long) {
            cards += Card(0, word.id, senseId, skill, CardState.NEW, due, 0.0, 0.0, 0, 0, null, now)
        }
        word.senses.forEachIndexed { i, s ->
            if (i == 0) { add(Skill.RECOGNIZE, s.id, now); add(Skill.CONTEXT, s.id, now + 5 * MIN) }
            else { add(Skill.RECOGNIZE, s.id, now + i * DAY); add(Skill.CONTEXT, s.id, now + (i + 1) * DAY) }
        }
        add(Skill.RECALL, null, now + DAY)
        if (word.isCore && word.allCollocations.size >= 2) add(Skill.USE, null, now + 3 * DAY)
        return cards
    }
}
