package dev.brown.wordy.domain.srs

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * FSRS-6 스케줄러 (Free Spaced Repetition Scheduler, https://github.com/open-spaced-repetition/fsrs4anki/wiki/The-Algorithm).
 * py-fsrs 의 동작을 따르되 의존성 없이 구현했다. 시간은 epoch millis, 간격은 일(day) 단위 실수.
 *
 * @param desiredRetention 목표 보존률 (0.7~0.97). 기본 0.9
 * @param learningSteps 새 카드의 단기 단계(분). 기본 1분, 10분
 * @param relearningSteps 망각 후 재학습 단계(분). 기본 10분
 */
class Fsrs(
    val desiredRetention: Double = 0.9,
    private val w: DoubleArray = DEFAULT_PARAMETERS,
    private val learningSteps: List<Double> = listOf(1.0, 10.0),
    private val relearningSteps: List<Double> = listOf(10.0),
    private val maximumIntervalDays: Int = 36500,
) {
    init { require(w.size == 21) { "FSRS-6 requires 21 parameters" } }

    private val decay = -w[20]
    private val factor = 0.9.pow(1 / decay) - 1

    /** 경과 시간 t(일)와 안정성 S 에 대한 회상 확률 */
    fun retrievability(elapsedDays: Double, stability: Double): Double =
        if (stability <= 0) 0.0 else (1 + factor * elapsedDays / stability).pow(decay)

    /** 현재 시각 기준 카드의 회상 확률 (NEW 카드는 0) */
    fun retrievability(card: FsrsCard, now: Long): Double {
        val last = card.lastReview ?: return 0.0
        return retrievability(daysBetween(last, now), card.stability)
    }

    /** 목표 보존률을 만족하는 다음 간격(일, 1 이상 정수) */
    fun nextInterval(stability: Double): Int {
        val ivl = stability / factor * (desiredRetention.pow(1 / decay) - 1)
        return ivl.roundToLong().coerceIn(1L, maximumIntervalDays.toLong()).toInt()
    }

    fun review(card: FsrsCard, rating: Rating, now: Long): ReviewResult {
        val elapsedDays = card.lastReview?.let { daysBetween(it, now) } ?: 0.0
        var s = card.stability
        var d = card.difficulty
        var state = card.state
        var step = card.step
        var lapses = card.lapses
        val scheduledDays: Double

        when (state) {
            CardState.NEW, CardState.LEARNING -> {
                if (state == CardState.NEW || card.lastReview == null) {
                    s = initialStability(rating); d = initialDifficulty(rating); state = CardState.LEARNING
                } else if (elapsedDays < 1) {
                    s = shortTermStability(s, rating); d = nextDifficulty(d, rating)
                } else {
                    s = nextStability(d, s, retrievability(elapsedDays, s), rating); d = nextDifficulty(d, rating)
                }
                val steps = learningSteps
                if (steps.isEmpty() || (step >= steps.size && rating != Rating.AGAIN)) {
                    state = CardState.REVIEW; step = 0; scheduledDays = nextInterval(s).toDouble()
                } else {
                    when (rating) {
                        Rating.AGAIN -> { step = 0; scheduledDays = minutes(steps[0]) }
                        Rating.HARD -> scheduledDays = when {
                            step == 0 && steps.size == 1 -> minutes(steps[0] * 1.5)
                            step == 0 -> minutes((steps[0] + steps[1]) / 2)
                            else -> minutes(steps[min(step, steps.size - 1)])
                        }
                        Rating.GOOD -> if (step + 1 >= steps.size) {
                            state = CardState.REVIEW; step = 0; scheduledDays = nextInterval(s).toDouble()
                        } else { step += 1; scheduledDays = minutes(steps[step]) }
                        Rating.EASY -> { state = CardState.REVIEW; step = 0; scheduledDays = nextInterval(s).toDouble() }
                    }
                }
            }
            CardState.REVIEW -> {
                val r = retrievability(elapsedDays, s)
                s = if (elapsedDays < 1) shortTermStability(s, rating) else nextStability(d, s, r, rating)
                d = nextDifficulty(d, rating)
                if (rating == Rating.AGAIN) {
                    lapses += 1
                    if (relearningSteps.isEmpty()) scheduledDays = nextInterval(s).toDouble()
                    else { state = CardState.RELEARNING; step = 0; scheduledDays = minutes(relearningSteps[0]) }
                } else scheduledDays = nextInterval(s).toDouble()
            }
            CardState.RELEARNING -> {
                val r = retrievability(elapsedDays, s)
                s = if (elapsedDays < 1) shortTermStability(s, rating) else nextStability(d, s, r, rating)
                d = nextDifficulty(d, rating)
                val steps = relearningSteps
                if (steps.isEmpty() || (step >= steps.size && rating != Rating.AGAIN)) {
                    state = CardState.REVIEW; step = 0; scheduledDays = nextInterval(s).toDouble()
                } else when (rating) {
                    Rating.AGAIN -> { step = 0; scheduledDays = minutes(steps[0]) }
                    Rating.HARD -> scheduledDays = when {
                        step == 0 && steps.size == 1 -> minutes(steps[0] * 1.5)
                        step == 0 -> minutes((steps[0] + steps[1]) / 2)
                        else -> minutes(steps[min(step, steps.size - 1)])
                    }
                    Rating.GOOD -> if (step + 1 >= steps.size) {
                        state = CardState.REVIEW; step = 0; scheduledDays = nextInterval(s).toDouble()
                    } else { step += 1; scheduledDays = minutes(steps[step]) }
                    Rating.EASY -> { state = CardState.REVIEW; step = 0; scheduledDays = nextInterval(s).toDouble() }
                }
            }
        }
        val updated = card.copy(
            state = state, step = step, stability = s, difficulty = d,
            reps = card.reps + 1, lapses = lapses, lastReview = now,
            due = now + (scheduledDays * DAY_MS).roundToLong(),
        )
        return ReviewResult(updated, scheduledDays, elapsedDays)
    }

    /** 네 등급 각각의 예상 간격(일). UI 의 "다음 복습 5일 후" 미리보기용 */
    fun preview(card: FsrsCard, now: Long): Map<Rating, Double> = Rating.entries.associateWith { review(card, it, now).scheduledDays }

    // ---- FSRS-6 formulas -------------------------------------------------------------------
    internal fun initialStability(rating: Rating): Double = max(w[rating.value - 1], STABILITY_MIN)

    internal fun initialDifficulty(rating: Rating): Double = clampDifficulty(w[4] - exp(w[5] * (rating.value - 1)) + 1)

    internal fun nextDifficulty(d: Double, rating: Rating): Double {
        val delta = -w[6] * (rating.value - 3)
        val damped = d + delta * (10 - d) / 9
        val meanReverted = w[7] * initialDifficultyRaw(Rating.EASY) + (1 - w[7]) * damped
        return clampDifficulty(meanReverted)
    }

    internal fun nextStability(d: Double, s: Double, r: Double, rating: Rating): Double =
        if (rating == Rating.AGAIN) forgetStability(d, s, r) else recallStability(d, s, r, rating)

    internal fun recallStability(d: Double, s: Double, r: Double, rating: Rating): Double {
        val hardPenalty = if (rating == Rating.HARD) w[15] else 1.0
        val easyBonus = if (rating == Rating.EASY) w[16] else 1.0
        val next = s * (exp(w[8]) * (11 - d) * s.pow(-w[9]) * (exp(w[10] * (1 - r)) - 1) * hardPenalty * easyBonus + 1)
        return clampStability(next)
    }

    internal fun forgetStability(d: Double, s: Double, r: Double): Double {
        val longTerm = w[11] * d.pow(-w[12]) * ((s + 1).pow(w[13]) - 1) * exp(w[14] * (1 - r))
        val shortTerm = s / exp(w[17] * w[18])
        return clampStability(min(longTerm, shortTerm))
    }

    internal fun shortTermStability(s: Double, rating: Rating): Double {
        var sinc = exp(w[17] * (rating.value - 3 + w[18])) * s.pow(-w[19])
        if (rating == Rating.GOOD || rating == Rating.EASY) sinc = max(sinc, 1.0)
        return clampStability(s * sinc)
    }

    private fun initialDifficultyRaw(rating: Rating) = w[4] - exp(w[5] * (rating.value - 1)) + 1
    private fun clampDifficulty(d: Double) = d.coerceIn(1.0, 10.0)
    private fun clampStability(s: Double) = s.coerceIn(STABILITY_MIN, maximumIntervalDays.toDouble())
    private fun minutes(m: Double) = m / (24 * 60)

    companion object {
        const val DAY_MS = 86_400_000.0
        const val STABILITY_MIN = 0.001
        fun daysBetween(fromMillis: Long, toMillis: Long): Double = max(0.0, (toMillis - fromMillis) / DAY_MS)

        /** FSRS-6 기본 파라미터 (py-fsrs 6.x DEFAULT_PARAMETERS) */
        val DEFAULT_PARAMETERS = doubleArrayOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796,
            1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542,
        )
    }
}
