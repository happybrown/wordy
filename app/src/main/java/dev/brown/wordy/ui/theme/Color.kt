package dev.brown.wordy.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Wordy 팔레트.
 *
 * 규칙 하나: 흰 바탕에 파랑 하나만 강조색으로 쓰고 나머지는 전부 회색.
 * 색이 여러 개면 무엇이 중요한지 알 수 없다. 색은 히어로 카드와
 * 아이콘 타일에만 들어가고, 본문·카드·구분선은 전부 무채색이다.
 */

// 강조색. 버튼과 선택 상태에만 쓴다.
val AccentBlue = Color(0xFF3B6EF5)
val AccentBlueDark = Color(0xFF8FAEFF)

// 히어로 카드 그라데이션. 보라 계열 한 쌍.
val HeroStart = Color(0xFF8B7CF6)
val HeroEnd = Color(0xFF6D5AE0)
val HeroStartDark = Color(0xFF5B4FA8)
val HeroEndDark = Color(0xFF453B87)

// 라이트: 순백에 가까운 바탕. 카드는 그보다 더 흰색으로 떠 보인다.
val LightBg = Color(0xFFF6F7FB)
val LightCard = Color(0xFFFFFFFF)
val LightCardAlt = Color(0xFFF0F2F8)
val LightInk = Color(0xFF161822)
val LightInkDim = Color(0xFF7C8398)
val LightLine = Color(0xFFE6E9F2)

// 다크: 푸른기가 도는 먹색.
val DarkBg = Color(0xFF111319)
val DarkCard = Color(0xFF1A1D26)
val DarkCardAlt = Color(0xFF232733)
val DarkInk = Color(0xFFECEEF5)
val DarkInkDim = Color(0xFF959BAD)
val DarkLine = Color(0xFF2C3140)

/**
 * 아이콘 타일 색. 참고 이미지처럼 아주 옅은 파스텔 배경에 진한 아이콘.
 * 레벨 5개 + Core 에 하나씩 배정한다. 이 색들은 분류를 나타낼 뿐
 * 강조가 아니므로 채도를 낮게 유지한다.
 */
val TileBackgrounds = listOf(
    Color(0xFFE7EDFF), // 파랑
    Color(0xFFFFE8EE), // 분홍
    Color(0xFFFFF1DE), // 주황
    Color(0xFFE3F6EC), // 초록
    Color(0xFFEFE9FF), // 보라
    Color(0xFFE1F4FA), // 하늘
)
val TileForegrounds = listOf(
    Color(0xFF3B6EF5),
    Color(0xFFE05580),
    Color(0xFFD4881F),
    Color(0xFF26A167),
    Color(0xFF7A55D6),
    Color(0xFF2091BC),
)

/** 다크용 타일. 옅은 파스텔을 어두운 바닥에 그대로 쓰면 눈이 부시다. */
val TileBackgroundsDark = listOf(
    Color(0xFF212942),
    Color(0xFF38222C),
    Color(0xFF382C1B),
    Color(0xFF1B3126),
    Color(0xFF2A2340),
    Color(0xFF1A2D36),
)
val TileForegroundsDark = listOf(
    Color(0xFF93AEFF),
    Color(0xFFFF8FAC),
    Color(0xFFFFBC64),
    Color(0xFF5FD09B),
    Color(0xFFB394FF),
    Color(0xFF6CC9E8),
)

/**
 * 숙달 단계(0~4). 회색 → 파랑 → 초록.
 * 색과 함께 명도도 올라가 색각 이상에서도 단계가 구분된다.
 */
val MasteryColors = listOf(
    Color(0xFFD2D7E3),
    Color(0xFFA6BCFA),
    Color(0xFF5C87F0),
    Color(0xFF3FBF85),
    Color(0xFF1E9463),
)

val MasteryColorsDark = listOf(
    Color(0xFF454B5E),
    Color(0xFF7B90DC),
    Color(0xFF7FA0FF),
    Color(0xFF55C694),
    Color(0xFF7EDCB0),
)
