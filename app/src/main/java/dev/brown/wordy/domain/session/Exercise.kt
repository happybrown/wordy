package dev.brown.wordy.domain.session

import dev.brown.wordy.domain.model.Card
import dev.brown.wordy.domain.model.Example
import dev.brown.wordy.domain.model.Sense
import dev.brown.wordy.domain.model.Word

enum class ExerciseType(val ko: String) {
    MEANING_CHOICE("뜻 고르기"),
    CLOZE_CHOICE("빈칸 고르기"),
    CLOZE_TYPE("빈칸 쓰기"),
    RECALL_TYPE("영어로 쓰기"),
    LISTEN_TYPE("듣고 쓰기"),
    COLLOCATION_MATCH("연어 짝 맞추기"),
    SENTENCE_SCRAMBLE("문장 조립"),
}

sealed interface Exercise {
    val card: Card
    val word: Word
    val type: ExerciseType

    /** 영어 단어 → 한국어 뜻 4지선다 */
    data class MeaningChoice(
        override val card: Card, override val word: Word, val sense: Sense,
        val options: List<String>, val correctIndex: Int,
    ) : Exercise { override val type = ExerciseType.MEANING_CHOICE }

    /** 예문 빈칸. options == null 이면 타이핑 */
    data class Cloze(
        override val card: Card, override val word: Word, val sense: Sense, val example: Example,
        val blanked: String, val answer: String, val options: List<String>?, val correctIndex: Int,
    ) : Exercise { override val type = if (options == null) ExerciseType.CLOZE_TYPE else ExerciseType.CLOZE_CHOICE }

    /** 한국어 뜻(+영어 정의) → 영어 타이핑. listen == true 면 TTS 로 단어를 들려주고 받아쓴다 */
    data class Recall(
        override val card: Card, override val word: Word, val glosses: List<String>, val definition: String, val listen: Boolean,
    ) : Exercise { override val type = if (listen) ExerciseType.LISTEN_TYPE else ExerciseType.RECALL_TYPE }

    /** 연어 짝 맞추기: left(빈칸 처리된 연어) ↔ right(headword) */
    data class CollocationMatch(
        override val card: Card, override val word: Word, val pairs: List<Pair<String, String>>,
    ) : Exercise { override val type = ExerciseType.COLLOCATION_MATCH }

    /** 문장 조립: shuffled 를 answer 순서로 */
    data class SentenceScramble(
        override val card: Card, override val word: Word, val example: Example, val shuffled: List<String>, val answer: List<String>,
    ) : Exercise { override val type = ExerciseType.SENTENCE_SCRAMBLE }
}

/** 사용자의 답 판정 결과 */
data class AnswerOutcome(val correct: Boolean, val almost: Boolean = false, val usedHint: Boolean = false, val elapsedMs: Long)
