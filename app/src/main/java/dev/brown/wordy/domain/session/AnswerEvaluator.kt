package dev.brown.wordy.domain.session

/** 사용자의 답. 정답 판정은 AnswerEvaluator 가 중앙에서 한다. */
sealed interface Answer {
    data class Choice(val index: Int) : Answer
    data class Text(val input: String, val usedHint: Boolean) : Answer
    data class Pairs(val mismatches: Int) : Answer
    data class Order(val chunks: List<String>) : Answer
}

object AnswerEvaluator {
    fun evaluate(ex: Exercise, answer: Answer, elapsedMs: Long, strict: Boolean = false): AnswerOutcome {
        val raw = when (ex) {
            is Exercise.MeaningChoice -> AnswerOutcome(correct = (answer as? Answer.Choice)?.index == ex.correctIndex, elapsedMs = elapsedMs)
            is Exercise.Cloze -> when (answer) {
                is Answer.Choice -> AnswerOutcome(correct = answer.index == ex.correctIndex, elapsedMs = elapsedMs)
                is Answer.Text -> typed(answer, listOf(ex.answer), ex.word.allForms, elapsedMs)
                else -> AnswerOutcome(false, elapsedMs = elapsedMs)
            }
            is Exercise.Recall -> (answer as? Answer.Text)?.let { typed(it, listOf(ex.word.headword), ex.word.allForms, elapsedMs) } ?: AnswerOutcome(false, elapsedMs = elapsedMs)
            is Exercise.CollocationMatch -> { val m = (answer as? Answer.Pairs)?.mismatches ?: 99; AnswerOutcome(correct = m == 0, almost = m == 1, elapsedMs = elapsedMs) }
            is Exercise.SentenceScramble -> {
                val o = (answer as? Answer.Order)?.chunks ?: emptyList()
                val misplaced = ex.answer.indices.count { i -> o.getOrNull(i) != ex.answer[i] }
                AnswerOutcome(correct = o == ex.answer, almost = misplaced in 1..2, elapsedMs = elapsedMs)
            }
        }
        return if (strict && raw.almost) raw.copy(almost = false) else raw
    }

    /** 정확한 형태가 정답, 다른 변화형이나 1글자 오타는 "거의 맞음" */
    private fun typed(t: Answer.Text, accepted: List<String>, allForms: List<String>, elapsedMs: Long): AnswerOutcome {
        val r = AnswerChecker.check(t.input, accepted)
        val otherForm = !r.correct && AnswerChecker.check(t.input, allForms).correct
        return AnswerOutcome(r.correct, almost = r.almost || otherForm, usedHint = t.usedHint, elapsedMs = elapsedMs)
    }

    fun correctAnswerOf(ex: Exercise): String = when (ex) {
        is Exercise.MeaningChoice -> ex.options[ex.correctIndex]
        is Exercise.Cloze -> ex.answer
        is Exercise.Recall -> ex.word.headword
        is Exercise.CollocationMatch -> ex.pairs.joinToString("  ·  ") { (l, r) -> l.replace(ExerciseGenerator.BLANK, r) }
        is Exercise.SentenceScramble -> ex.answer.joinToString(" ")
    }
}
