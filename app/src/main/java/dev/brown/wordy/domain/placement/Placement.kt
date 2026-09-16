package dev.brown.wordy.domain.placement

import dev.brown.wordy.domain.model.Level

/**
 * 레벨 진단. 레벨마다 같은 수의 문제를 내고, 정답률이 기준을 넘는 가장 높은 레벨의
 * **다음 레벨**을 시작점으로 추천한다.
 *
 * 다음 레벨을 주는 이유: 초등 문제를 다 맞혔다면 초등부터 시작할 이유가 없다.
 * 이미 아는 단어를 다시 훑는 것은 시간 낭비이고 금방 지루해진다.
 */
object Placement {
    /** 이 비율 이상 맞히면 그 레벨은 "안다"고 본다. */
    const val PASS_RATIO = 0.7

    /**
     * @param correctByLevel 레벨별 정답 수
     * @param askedByLevel 레벨별 출제 수. 0인 레벨은 판단에서 제외한다.
     */
    fun recommend(correctByLevel: Map<Level, Int>, askedByLevel: Map<Level, Int>): Level {
        // 통과한 가장 높은 레벨을 찾는다. 낮은 레벨을 틀리고 높은 레벨을 맞히는 경우도
        // 있으므로(찍었거나 특정 분야만 아는 경우) 연속성은 요구하지 않는다.
        val passed = Level.entries.filter { lvl ->
            val asked = askedByLevel[lvl] ?: 0
            asked > 0 && (correctByLevel[lvl] ?: 0).toDouble() / asked >= PASS_RATIO
        }
        val highestPassed = passed.maxByOrNull { it.ordinal } ?: return Level.entries.first()
        // 최고 레벨까지 통과했으면 더 올릴 곳이 없으니 그대로 둔다.
        return Level.entries.getOrElse(highestPassed.ordinal + 1) { highestPassed }
    }

    /** 진단 결과를 사용자에게 한 줄로 설명한다. */
    fun summary(correctByLevel: Map<Level, Int>, askedByLevel: Map<Level, Int>, recommended: Level): String {
        val total = askedByLevel.values.sum()
        val correct = correctByLevel.values.sum()
        if (total == 0) return "진단을 건너뛰었습니다."
        val perLevel = Level.entries
            .filter { (askedByLevel[it] ?: 0) > 0 }
            .joinToString(" · ") { "${it.shortKo} ${correctByLevel[it] ?: 0}/${askedByLevel[it]}" }
        return "${total}문제 중 ${correct}개 정답 ($perLevel) → ${recommended.ko} 레벨을 추천합니다."
    }
}
