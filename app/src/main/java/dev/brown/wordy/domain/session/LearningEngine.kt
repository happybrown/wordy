package dev.brown.wordy.domain.session

import dev.brown.wordy.data.prefs.SettingsRepository
import dev.brown.wordy.data.repo.ProgressRepository
import dev.brown.wordy.data.repo.WordRepository
import dev.brown.wordy.domain.mastery.MasteryCalculator
import dev.brown.wordy.domain.model.Card
import dev.brown.wordy.domain.model.Mastery
import dev.brown.wordy.domain.model.Word
import dev.brown.wordy.domain.model.WordStatus
import dev.brown.wordy.domain.srs.Fsrs
import dev.brown.wordy.domain.srs.Rating

/** 학습 이벤트(단어 도입, 답 제출)를 받아 FSRS·숙달·일일 통계를 일관되게 갱신하는 유스케이스. */
class LearningEngine(
    private val words: WordRepository,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
) {
    private suspend fun fsrs() = Fsrs(desiredRetention = settings.current().desiredRetention.toDouble())

    /** 새 단어 도입: 카드 생성 + 상태 '만남' + 오늘 신규 +1. 이미 도입된 단어면 기존 카드를 돌려준다. */
    suspend fun introduce(word: Word, now: Long): List<Card> {
        val existing = progress.cardsForWord(word.id)
        if (existing.isNotEmpty()) return existing
        progress.insertCards(CardFactory.cardsFor(word, now))
        val cur = progress.status(word.id)
        progress.upsertStatus(WordStatus(word.id, Mastery.MET, now, null, cur?.isBookmarked ?: false, cur?.note), now)
        progress.bumpDaily(ProgressRepository.today(now), newLearned = 1)
        return progress.cardsForWord(word.id)
    }

    data class Answered(val card: Card, val rating: Rating, val scheduledDays: Double, val mastery: Mastery)

    /** 답 제출 → 자동 등급 → FSRS 갱신 → 숙달 재계산 → 일일 통계 */
    suspend fun answer(exercise: Exercise, outcome: AnswerOutcome, now: Long): Answered {
        val rating = AutoRater.rate(outcome, exercise.type)
        return review(exercise.card, rating, exercise.type.name, outcome.correct, outcome.elapsedMs, now)
    }

    /** 플래시카드 자기평가 등 등급이 직접 주어지는 경우 */
    suspend fun review(card: Card, rating: Rating, exerciseType: String, wasCorrect: Boolean, elapsedMs: Long, now: Long): Answered {
        val result = fsrs().review(card.toFsrs(), rating, now)
        val updated = progress.applyReview(card, result.card, rating, exerciseType, wasCorrect, elapsedMs, result.scheduledDays, now)
        val mastery = recomputeMastery(card.wordId, now)
        progress.bumpDaily(ProgressRepository.today(now), reviewed = 1, correct = if (wasCorrect) 1 else 0, seconds = (elapsedMs / 1000).toInt().coerceIn(0, 120))
        return Answered(updated, rating, result.scheduledDays, mastery)
    }

    suspend fun recomputeMastery(wordId: String, now: Long): Mastery {
        val cards = progress.cardsForWord(wordId)
        val mastery = MasteryCalculator.compute(cards)
        val cur = progress.status(wordId)
        val masteredAt = if (mastery == Mastery.MASTERED) cur?.masteredAt ?: now else null
        progress.upsertStatus(WordStatus(wordId, mastery, cur?.introducedAt ?: now, masteredAt, cur?.isBookmarked ?: false, cur?.note), now)
        return mastery
    }

    suspend fun previewIntervals(card: Card, now: Long): Map<Rating, Double> = fsrs().preview(card.toFsrs(), now)
}
