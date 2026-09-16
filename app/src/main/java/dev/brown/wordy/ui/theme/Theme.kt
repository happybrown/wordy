package dev.brown.wordy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2EAFF),
    onPrimaryContainer = Color(0xFF122356),
    secondary = Color(0xFF6D5AE0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E3FF),
    onSecondaryContainer = Color(0xFF241B52),
    tertiary = Color(0xFFD4881F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF1DE),
    onTertiaryContainer = Color(0xFF4A2F06),
    background = LightBg,
    onBackground = LightInk,
    surface = LightCard,
    onSurface = LightInk,
    surfaceContainer = LightCard,
    surfaceContainerHigh = LightCardAlt,
    surfaceContainerLow = LightCard,
    surfaceVariant = LightCardAlt,
    onSurfaceVariant = LightInkDim,
    outline = LightLine,
    outlineVariant = Color(0xFFEDEFF6),
    error = Color(0xFFD03B2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFE3E0),
    onErrorContainer = Color(0xFF5A130D),
)

private val DarkColors = darkColorScheme(
    primary = AccentBlueDark,
    onPrimary = Color(0xFF122356),
    primaryContainer = Color(0xFF26356E),
    onPrimaryContainer = Color(0xFFDCE4FF),
    secondary = Color(0xFFB9A9FF),
    onSecondary = Color(0xFF241B52),
    secondaryContainer = Color(0xFF362C68),
    onSecondaryContainer = Color(0xFFE6DEFF),
    tertiary = Color(0xFFFFBC64),
    onTertiary = Color(0xFF4A2F06),
    tertiaryContainer = Color(0xFF55401B),
    onTertiaryContainer = Color(0xFFFFE5C2),
    background = DarkBg,
    onBackground = DarkInk,
    surface = DarkCard,
    onSurface = DarkInk,
    surfaceContainer = DarkCard,
    surfaceContainerHigh = DarkCardAlt,
    surfaceContainerLow = DarkCard,
    surfaceVariant = DarkCardAlt,
    onSurfaceVariant = DarkInkDim,
    outline = DarkLine,
    outlineVariant = Color(0xFF262B38),
    error = Color(0xFFFF9B90),
    onError = Color(0xFF5A130D),
    errorContainer = Color(0xFF6B2822),
    onErrorContainer = Color(0xFFFFD8D3),
)

/**
 * 모서리. 참고 이미지처럼 카드는 크게 굴리고 작은 칩·타일은 그보다 덜 굴린다.
 * 전부 같은 값으로 두면 위계가 사라진다.
 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * 앱 테마.
 *
 * **시스템 다크 모드를 따라가지 않고 항상 밝은 화면으로 고정한다.**
 * 밝고 흰 바탕이 이 앱이 목표한 인상이고, 다크로 뒤집으면 파스텔 타일과
 * 그라데이션이 전부 탁해져 다른 앱처럼 보인다. 기기 설정이 다크여도
 * 여기서는 라이트 팔레트만 쓴다.
 *
 * 다이나믹 컬러(기기 배경화면 색)도 쓰지 않는다. 기기마다 색이 달라지면
 * "파랑 하나만 강조색" 이라는 규칙이 무너진다.
 */
@Composable
fun WordyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = AppShapes,
        typography = Typography,
        content = content,
    )
}

// 팔레트를 라이트로 고정했으므로 아래 접근자들은 항상 라이트 값을 준다.
// DarkColors 와 *Dark 색 목록은 나중에 다크 테마를 되살릴 때를 위해 남겨 둔다.
private const val isDark = false

val masteryColors: List<Color> get() = MasteryColors

val tileBackgrounds: List<Color> get() = TileBackgrounds

val tileForegrounds: List<Color> get() = TileForegrounds

/** 히어로 카드 그라데이션. 화면당 한 곳에만 쓴다. */
val heroGradient: Brush get() = Brush.linearGradient(listOf(HeroStart, HeroEnd))

val onHeroGradient: Color get() = Color.White
