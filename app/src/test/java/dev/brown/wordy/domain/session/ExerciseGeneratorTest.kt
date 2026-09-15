package dev.brown.wordy.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseGeneratorTest {
    private val forms = listOf("get", "gets", "got", "gotten", "getting")

    @Test fun `blankOut replaces the form inside the focus span`() {
        val r = ExerciseGenerator.blankOut("I got a text from Mina this morning.", "got a text", forms)!!
        assertEquals("I ______ a text from Mina this morning.", r.first)
        assertEquals("got", r.second)
    }

    @Test fun `blankOut prefers the longest matching form and respects word boundaries`() {
        val r = ExerciseGenerator.blankOut("Getting there takes an hour; forget it.", "Getting there", forms)!!
        assertEquals("______ there takes an hour; forget it.", r.first)
        assertEquals("Getting", r.second)
    }

    @Test fun `blankOut returns null when no form is present`() {
        assertNull(ExerciseGenerator.blankOut("Nothing here.", "Nothing", forms))
    }

    @Test fun `blankOut falls back to any occurrence when focus does not contain the form`() {
        val r = ExerciseGenerator.blankOut("Can you get some milk?", "some milk", forms)!!
        assertEquals("Can you ______ some milk?", r.first)
    }
}

/**
 * 연어 짝 맞추기는 오른쪽 보기(단어)가 서로 달라야 문제가 성립한다.
 * 같은 단어가 두 번 나오면 어느 빈칸에 넣어야 할지 결정할 수 없다.
 */
class CollocationPairsTest {
    @Test fun `duplicate right-hand options make the exercise unsolvable`() {
        val ambiguous = listOf("______ home late" to "get", "______ a job" to "get", "______ down" to "climb")
        val distinct = ambiguous.map { it.second }.distinct()
        assertEquals("중복이 있으면 보기 수가 줄어든다", 2, distinct.size)
        assertTrue("생성기는 이런 조합을 만들면 안 된다", ambiguous.size != distinct.size)
    }

    @Test fun `a valid set has one option per blank`() {
        val valid = listOf("______ home late" to "get", "______ a job" to "find", "______ down" to "climb")
        assertEquals(valid.size, valid.map { it.second }.distinct().size)
    }
}
