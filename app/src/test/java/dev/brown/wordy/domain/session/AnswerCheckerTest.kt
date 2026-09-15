package dev.brown.wordy.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCheckerTest {
    @Test fun `exact match ignores case and whitespace`() {
        assertTrue(AnswerChecker.check("  Breakfast ", listOf("breakfast")).correct)
        assertTrue(AnswerChecker.check("ice  cream", listOf("ice cream")).correct)
        assertTrue(AnswerChecker.check("don’t", listOf("don't")).correct)
    }

    @Test fun `one letter typo is almost, not correct`() {
        val r = AnswerChecker.check("breakfest", listOf("breakfast"))
        assertFalse(r.correct); assertTrue(r.almost)
    }

    @Test fun `short words do not get typo tolerance`() {
        val r = AnswerChecker.check("cot", listOf("cat"))
        assertFalse(r.correct); assertFalse(r.almost)
    }

    @Test fun `strict mode disables almost`() {
        val r = AnswerChecker.check("breakfest", listOf("breakfast"), strict = true)
        assertFalse(r.correct); assertFalse(r.almost)
    }

    @Test fun `empty input is wrong`() {
        assertFalse(AnswerChecker.check("   ", listOf("get")).correct)
    }

    @Test fun `levenshtein basics`() {
        assertEquals(0, AnswerChecker.levenshtein("get", "get"))
        assertEquals(1, AnswerChecker.levenshtein("get", "got"))
        assertEquals(3, AnswerChecker.levenshtein("", "abc"))
    }
}
