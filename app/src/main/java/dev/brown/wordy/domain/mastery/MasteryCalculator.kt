package dev.brown.wordy.domain.mastery

import dev.brown.wordy.domain.model.Card
import dev.brown.wordy.domain.model.Mastery
import dev.brown.wordy.domain.srs.CardState

/**
 * 단어의 모든 카드로부터 숙달 단계를 계산한다.
 * 3 익숙: 모든 카드가 REVIEW 상태이고 최소 stability ≥ 7일
 * 4 숙달: 모든 카드가 REVIEW 상태이고 최소 stability ≥ 30일
 * Core 단어는 sense 마다 카드가 있으므로 자연히 더 엄격하다.
 */
object MasteryCalculator {
    const val FAMILIAR_DAYS = 7.0
    const val MASTERED_DAYS = 30.0

    fun compute(cards: List<Card>): Mastery {
        if (cards.isEmpty()) return Mastery.NEW
        if (cards.all { it.reps == 0 }) return Mastery.MET
        val allReview = cards.all { it.state == CardState.REVIEW }
        val minStability = cards.minOf { it.stability }
        return when {
            allReview && minStability >= MASTERED_DAYS -> Mastery.MASTERED
            allReview && minStability >= FAMILIAR_DAYS -> Mastery.FAMILIAR
            else -> Mastery.LEARNING
        }
    }

    /** 0.0~1.0 진행률: 카드 평균 (state 가중 + stability 비율) — 진행 링 표시용 */
    fun progress(cards: List<Card>): Float {
        if (cards.isEmpty()) return 0f
        val perCard = cards.map { c ->
            val base = when (c.state) { CardState.NEW -> 0.0; CardState.LEARNING, CardState.RELEARNING -> 0.25; CardState.REVIEW -> 0.5 }
            (base + 0.5 * (c.stability / MASTERED_DAYS).coerceIn(0.0, 1.0)).coerceIn(0.0, 1.0)
        }
        return perCard.average().toFloat()
    }
}
