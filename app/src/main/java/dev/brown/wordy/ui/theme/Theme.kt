package dev.brown.wordy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

@Composable
fun WordyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // 다이나믹 컬러를 쓰지 않는다. 기기 배경화면 색을 따라가면 앱마다 달라져
    // 강조색 하나로 통일한다는 규칙이 무너진다.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        typography = Typography,
        content = content,
    )
}

private val isDark: Boolean
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

val masteryColors: List<Color>
    @Composable @ReadOnlyComposable
    get() = if (isDark) MasteryColorsDark else MasteryColors

val tileBackgrounds: List<Color>
    @Composable @ReadOnlyComposable
    get() = if (isDark) TileBackgroundsDark else TileBackgrounds

val tileForegrounds: List<Color>
    @Composable @ReadOnlyComposable
    get() = if (isDark) TileForegroundsDark else TileForegrounds

/** 히어로 카드 그라데이션. 화면당 한 곳에만 쓴다. */
val heroGradient: Brush
    @Composable @ReadOnlyComposable
    get() = if (isDark) {
        Brush.linearGradient(listOf(HeroStartDark, HeroEndDark))
    } else {
        Brush.linearGradient(listOf(HeroStart, HeroEnd))
    }

val onHeroGradient: Color
    @Composable @ReadOnlyComposable
    get() = Color.White
