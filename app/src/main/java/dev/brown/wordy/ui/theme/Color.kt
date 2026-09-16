package dev.brown.wordy.ui.theme

import androidx.compose.ui.graphics.Color

// 브랜드 색. 남색 계열 하나로 통일하고 채도를 낮춰 오래 봐도 피로하지 않게 했다.
// 학습 앱은 한 번에 20~30분씩 보는 화면이라 원색을 크게 쓰면 눈이 빨리 지친다.
val Indigo40 = Color(0xFF4059C8)
val Indigo80 = Color(0xFFB8C4FF)
val Teal40 = Color(0xFF00696E)
val Teal80 = Color(0xFF80D5DB)
val Amber40 = Color(0xFF8A5100)
val Amber80 = Color(0xFFFFB871)

// 라이트: 순백 대신 아주 옅은 청회색 바닥. 카드가 흰색으로 떠 보이게 하는 요즘 방식.
val LightBackground = Color(0xFFF7F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceContainer = Color(0xFFF0F2F9)
val LightSurfaceContainerHigh = Color(0xFFE9ECF6)

// 다크: 완전한 검정(#000)은 대비가 지나쳐 글을 읽기 힘들다. 살짝 푸른 먹색을 쓴다.
val DarkBackground = Color(0xFF111318)
val DarkSurface = Color(0xFF111318)
val DarkSurfaceContainer = Color(0xFF1D2024)
val DarkSurfaceContainerHigh = Color(0xFF272A2F)

/**
 * 숙달 단계(0~4) 색상.
 * 회색 → 파랑 → 초록으로 이어져 "모름에서 앎으로" 가 한눈에 읽힌다.
 * 색만으로 단계를 구분하지 않도록 명도도 함께 올라간다(색각 이상 고려).
 */
val MasteryColors = listOf(
    Color(0xFFB0B5C0), // 0 새 단어
    Color(0xFF8FB4F5), // 1 만남
    Color(0xFF5B8DEF), // 2 학습 중
    Color(0xFF52B788), // 3 익숙
    Color(0xFF2D8659), // 4 숙달
)

/** 다크 모드용 숙달 색. 어두운 바닥에서 라이트 색을 그대로 쓰면 탁해 보인다. */
val MasteryColorsDark = listOf(
    Color(0xFF6B7280),
    Color(0xFF7BA3E8),
    Color(0xFF6E9DF0),
    Color(0xFF5FC496),
    Color(0xFF7FD9AC),
)
