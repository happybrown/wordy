package dev.brown.wordy.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
