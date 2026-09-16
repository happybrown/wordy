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
    primary = BloomPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4EAFF),
    onPrimaryContainer = Color(0xFF16205C),
    secondary = BloomSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAE4FF),
    onSecondaryContainer = Color(0xFF241B4D),
    tertiary = BloomTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE6D9),
    onTertiaryContainer = Color(0xFF52210C),
    background = BloomBackground,
    onBackground = BloomOnSurface,
    surface = BloomSurface,
    onSurface = BloomOnSurface,
    surfaceContainer = BloomContainer,
    surfaceContainerHigh = BloomContainerHigh,
    surfaceContainerLow = BloomSurface,
    surfaceVariant = BloomContainerHigh,
    onSurfaceVariant = BloomOnSurfaceVariant,
    outline = BloomOutline,
    outlineVariant = Color(0xFFE7ECF7),
    error = Color(0xFFC0392B),
    onError = Color.White,
    errorContainer = Color(0xFFFFE0DC),
    onErrorContainer = Color(0xFF5C1710),
)

private val DarkColors = darkColorScheme(
    primary = BloomPrimaryDark,
    onPrimary = Color(0xFF16205C),
    primaryContainer = Color(0xFF2B3778),
    onPrimaryContainer = Color(0xFFDCE3FF),
    secondary = BloomSecondaryDark,
    onSecondary = Color(0xFF241B4D),
    secondaryContainer = Color(0xFF3A3170),
    onSecondaryContainer = Color(0xFFE4DEFF),
    tertiary = BloomTertiaryDark,
    onTertiary = Color(0xFF52210C),
    tertiaryContainer = Color(0xFF5E3520),
    onTertiaryContainer = Color(0xFFFFDCC9),
    background = BloomBackgroundDark,
    onBackground = BloomOnSurfaceDark,
    surface = BloomSurfaceDark,
    onSurface = BloomOnSurfaceDark,
    surfaceContainer = BloomContainerDark,
    surfaceContainerHigh = BloomContainerHighDark,
    surfaceContainerLow = BloomContainerDark,
    surfaceVariant = BloomContainerHighDark,
    onSurfaceVariant = BloomOnSurfaceVariantDark,
    outline = BloomOutlineDark,
    outlineVariant = Color(0xFF2E3348),
    error = Color(0xFFFF9C8F),
    onError = Color(0xFF5C1710),
    errorContainer = Color(0xFF6B2A22),
    onErrorContainer = Color(0xFFFFD6D0),
)

/**
 * Bloom 은 곡률이 정체성이다. Material 기본값보다 훨씬 크게 잡았고,
 * 버튼과 칩은 아예 완전한 알약 모양(CircleShape)을 직접 쓴다.
 */
private val BloomShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun WordyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // 다이나믹 컬러(기기 배경화면에서 뽑은 색)를 쓰지 않는다. 분홍 그라데이션이
    // 이 앱의 정체성인데 기기마다 색이 바뀌면 그 정체성이 사라진다.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = BloomShapes,
        typography = Typography,
        content = content,
    )
}

/** 현재 테마에 맞는 숙달 단계 색. */
val masteryColors: List<Color>
    @Composable @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MasteryColorsDark else MasteryColors

/**
 * Bloom 의 중심이 되는 그라데이션. 화면당 한 곳에만 쓴다.
 * 여러 군데 쓰면 알록달록해져 오히려 싸구려로 보인다.
 */
val bloomGradient: Brush
    @Composable @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        Brush.linearGradient(listOf(BloomBlueDark, BloomIndigoDark, BloomVioletDark))
    } else {
        Brush.linearGradient(listOf(BloomBlue, BloomIndigo, BloomViolet))
    }

/** 그라데이션 위에 얹는 글자색. 라이트와 다크 모두 흰색이 맞다. */
val onBloomGradient: Color
    @Composable @ReadOnlyComposable
    get() = Color.White

/** 현재 테마에 맞는 과목 타일 배경색. */
val tileBackgrounds: List<Color>
    @Composable @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) TileBackgroundsDark else TileBackgrounds

/** 현재 테마에 맞는 과목 타일 아이콘·글자색. */
val tileForegrounds: List<Color>
    @Composable @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) TileForegroundsDark else TileForegrounds
