package dev.brown.wordy.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Bloom Light 팔레트.
 *
 * 바탕을 거의 흰색으로 비우고 색은 히어로 카드와 아이콘 타일에만 넣는다.
 * 앞선 분홍 버전은 바탕까지 분홍기가 돌아 전체가 무거웠다. 여기서는
 * 바탕이 비어 있어야 카드의 색이 살아난다.
 */

// 히어로 그라데이션. 파랑 → 보라. 분홍 계열보다 밝고 시원하다.
val BloomBlue = Color(0xFF6C8BFF)
val BloomIndigo = Color(0xFF7B6CF6)
val BloomViolet = Color(0xFF9B6BE8)

// 다크용. 명도를 낮추되 색상은 유지해 같은 앱으로 읽히게 한다.
val BloomBlueDark = Color(0xFF4257B8)
val BloomIndigoDark = Color(0xFF4E43A8)
val BloomVioletDark = Color(0xFF63459B)

val BloomPrimary = Color(0xFF3D5AFE)
val BloomPrimaryDark = Color(0xFFAEBDFF)
val BloomSecondary = Color(0xFF7B6CF6)
val BloomSecondaryDark = Color(0xFFCBC2FF)
val BloomTertiary = Color(0xFFE8734A)
val BloomTertiaryDark = Color(0xFFFFB292)

// 라이트: 거의 흰색. 카드가 흰색으로 떠 보이도록 바탕만 아주 살짝 푸르게 둔다.
val BloomBackground = Color(0xFFF7F9FE)
val BloomSurface = Color(0xFFFFFFFF)
val BloomContainer = Color(0xFFFFFFFF)
val BloomContainerHigh = Color(0xFFEEF2FB)
val BloomOnSurface = Color(0xFF1B1D28)
val BloomOnSurfaceVariant = Color(0xFF8A90A6)
val BloomOutline = Color(0xFFD5DBEA)

// 다크: 푸른기가 도는 먹색.
val BloomBackgroundDark = Color(0xFF121420)
val BloomSurfaceDark = Color(0xFF121420)
val BloomContainerDark = Color(0xFF1C1F2E)
val BloomContainerHighDark = Color(0xFF262A3B)
val BloomOnSurfaceDark = Color(0xFFE9EBF4)
val BloomOnSurfaceVariantDark = Color(0xFF9BA2B8)
val BloomOutlineDark = Color(0xFF39405A)

/**
 * 과목 타일 색. 첨부 이미지의 파스텔 타일처럼 배경은 아주 옅게, 아이콘은 진하게.
 * 레벨 5개와 Core 에 하나씩 배정한다.
 */
val TileBackgrounds = listOf(
    Color(0xFFE8EDFF), // 파랑
    Color(0xFFFFE9EC), // 분홍
    Color(0xFFFFF2DC), // 주황
    Color(0xFFE4F7EC), // 초록
    Color(0xFFF0E9FF), // 보라
    Color(0xFFE2F5FA), // 하늘
)
val TileForegrounds = listOf(
    Color(0xFF3D5AFE),
    Color(0xFFE8567A),
    Color(0xFFE0891F),
    Color(0xFF2FA96B),
    Color(0xFF7B4FD6),
    Color(0xFF2596BE),
)

/** 다크용 타일. 옅은 파스텔을 그대로 쓰면 어두운 바닥에서 눈이 부시다. */
val TileBackgroundsDark = listOf(
    Color(0xFF232A45),
    Color(0xFF3A2430),
    Color(0xFF3A2E1C),
    Color(0xFF1D3328),
    Color(0xFF2C2440),
    Color(0xFF1C2F38),
)
val TileForegroundsDark = listOf(
    Color(0xFF9DB0FF),
    Color(0xFFFF93AC),
    Color(0xFFFFC069),
    Color(0xFF6ED3A0),
    Color(0xFFBC9BFF),
    Color(0xFF74CDEA),
)

/**
 * 숙달 단계(0~4) 색상. 회색 → 파랑 → 초록.
 * 색과 함께 명도도 올라가 색각 이상에서도 단계가 구분된다.
 */
val MasteryColors = listOf(
    Color(0xFFCED4E4),
    Color(0xFFA9BCFF),
    Color(0xFF6C8BFF),
    Color(0xFF45C88A),
    Color(0xFF229A66),
)

val MasteryColorsDark = listOf(
    Color(0xFF4A5170),
    Color(0xFF7D8FE0),
    Color(0xFF8AA3FF),
    Color(0xFF5CCB96),
    Color(0xFF84E0B4),
)
