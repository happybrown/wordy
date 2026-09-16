package dev.brown.wordy.domain.mastery

import java.time.LocalDate

/**
 * 연속 학습일. 오늘 아직 학습하지 않았어도 어제까지 이어졌으면 유지한다.
 * 저녁에 공부하는 사람이 아침에 앱을 열었다고 스트릭이 0으로 보이면 안 되기 때문이다.
 *
 * @param activeDates 실제로 학습한 날짜(ISO yyyy-MM-dd). 기록 행만 있고 학습량이 0인 날은 넣지 않는다.
 */
fun streakOf(activeDates: Set<String>, today: LocalDate): Int {
    var n = 0
    var d = if (activeDates.contains(today.toString())) today else today.minusDays(1)
    while (activeDates.contains(d.toString())) {
        n++
        d = d.minusDays(1)
    }
    return n
}
