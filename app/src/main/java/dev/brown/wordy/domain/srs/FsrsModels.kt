package dev.brown.wordy.domain.srs

enum class Rating(val value: Int) { AGAIN(1), HARD(2), GOOD(3), EASY(4);
    companion object { fun of(v: Int) = entries.first { it.value == v } }
}

enum class CardState { NEW, LEARNING, REVIEW, RELEARNING }

/**
 * 순수 FSRS 카드 상태. 저장 계층(CardEntity)과 분리되어 JVM 단위테스트가 가능하다.
 * @param due 다음 복습 시각(epoch millis)  @param step 학습/재학습 단계 인덱스(REVIEW 에서는 의미 없음)
 */
data class FsrsCard(
    val state: CardState = CardState.NEW,
    val due: Long,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val lastReview: Long? = null,
    val step: Int = 0,
)

data class ReviewResult(val card: FsrsCard, val scheduledDays: Double, val elapsedDays: Double)
