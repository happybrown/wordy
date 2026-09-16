package dev.brown.wordy.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Bloom 팔레트.
 *
 * 분홍에서 보라로 넘어가는 그라데이션이 이 디자인의 중심이다. 나머지 색은
 * 전부 그 뒤로 물러나야 하므로 채도를 낮추고 분홍기가 살짝 도는 중성색을 쓴다.
 * 순회색을 쓰면 그라데이션만 붕 떠 보인다.
 */

// 그라데이션 3점. 위에서 아래로 분홍 → 자주 → 보라.
val BloomPink = Color(0xFFF2789E)
val BloomMid = Color(0xFFC0568E)
val BloomPurple = Color(0xFF8C4A96)

// 다크용 그라데이션. 같은 색을 어두운 바닥에 얹으면 눈이 아파서 명도를 내리고
// 채도도 함께 낮춘다. 밤에 보는 화면이라는 점을 고려했다.
val BloomPinkDark = Color(0xFFB8496B)
val BloomMidDark = Color(0xFF8E3A66)
val BloomPurpleDark = Color(0xFF63336B)

val BloomPrimary = Color(0xFFB5518A)
val BloomPrimaryDark = Color(0xFFF7A8C8)
val BloomSecondary = Color(0xFF7A5B9E)
val BloomSecondaryDark = Color(0xFFCBB4E8)
val BloomTertiary = Color(0xFFC77D4A)
val BloomTertiaryDark = Color(0xFFFFB877)

// 라이트: 분홍기가 아주 옅게 도는 흰 바닥. 순백이면 카드가 안 떠 보인다.
val BloomBackground = Color(0xFFFDF6F8)
val BloomSurface = Color(0xFFFFFFFF)
val BloomContainer = Color(0xFFFFFFFF)
val BloomContainerHigh = Color(0xFFF9EDF2)
val BloomOnSurface = Color(0xFF2E1F26)
val BloomOnSurfaceVariant = Color(0xFF9B8189)
val BloomOutline = Color(0xFFD9C4CD)

// 다크: 검정 대신 자주기가 도는 먹색. 분홍 그라데이션과 같은 계열이라 겉돌지 않는다.
val BloomBackgroundDark = Color(0xFF171216)
val BloomSurfaceDark = Color(0xFF171216)
val BloomContainerDark = Color(0xFF231C22)
val BloomContainerHighDark = Color(0xFF2E252D)
val BloomOnSurfaceDark = Color(0xFFF2E7EC)
val BloomOnSurfaceVariantDark = Color(0xFFB09AA5)
val BloomOutlineDark = Color(0xFF473A44)

/**
 * 숙달 단계(0~4) 색상.
 * 분홍 팔레트 안에서 회색 → 분홍 → 초록으로 간다. 마지막 두 단계만 초록으로
 * 빼서 "다 익혔다" 는 신호가 그라데이션과 구분되게 했다.
 */
val MasteryColors = listOf(
    Color(0xFFD9D2D5), // 0 새 단어
    Color(0xFFF2B8CE), // 1 만남
    Color(0xFFE07BA6), // 2 학습 중
    Color(0xFF6BBF8A), // 3 익숙
    Color(0xFF3D9970), // 4 숙달
)

/** 다크 모드용 숙달 색. 어두운 바닥에서 라이트 색을 그대로 쓰면 탁해 보인다. */
val MasteryColorsDark = listOf(
    Color(0xFF5C4E56),
    Color(0xFFB8788F),
    Color(0xFFD97FA8),
    Color(0xFF6FC796),
    Color(0xFF8FDCB0),
)
