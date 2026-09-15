package dev.brown.wordy.domain.session

import dev.brown.wordy.domain.srs.Rating

/**
 * 퀴즈 결과 → FSRS 등급. 사용자는 판정 버튼을 누르지 않는다.
 * - 오답: Again
 * - 힌트 사용 또는 "거의 맞음"(1글자 오타): Hard
 * - 정답: Good, 빠르게 맞히면 Easy (선택형 3초, 타이핑 7초 이내)
 */
object AutoRater {
    fun rate(outcome: AnswerOutcome, type: ExerciseType): Rating = when {
        !outcome.correct && outcome.almost -> Rating.HARD
        !outcome.correct -> Rating.AGAIN
        outcome.usedHint -> Rating.HARD
        outcome.elapsedMs <= fastThresholdMs(type) -> Rating.EASY
        else -> Rating.GOOD
    }

    fun fastThresholdMs(type: ExerciseType): Long = when (type) {
        ExerciseType.MEANING_CHOICE, ExerciseType.CLOZE_CHOICE -> 3_000
        ExerciseType.RECALL_TYPE, ExerciseType.LISTEN_TYPE, ExerciseType.CLOZE_TYPE -> 7_000
        ExerciseType.COLLOCATION_MATCH, ExerciseType.SENTENCE_SCRAMBLE -> 12_000
    }
}
