package dev.brown.wordy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Indigo40,
    secondary = Teal40,
    tertiary = Amber40,
    background = LightBackground,
    surface = LightSurface,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerLow = LightSurface,
)

private val DarkColors = darkColorScheme(
    primary = Indigo80,
    secondary = Teal80,
    tertiary = Amber80,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerLow = DarkSurfaceContainer,
)

/**
 * 모서리 반경. Material 3 기본값보다 크게 잡았다.
 * 요즘 앱은 카드·버튼 모서리가 둥글수록 최신으로 읽히고, 한글처럼 획이 많은
 * 글자와 함께 놓였을 때 화면이 덜 빡빡해 보인다.
 */
private val WordyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun WordyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    // MaterialExpressiveTheme 과 MotionScheme 은 material3 1.4.0 에서 아직 internal 이라
    // 쓸 수 없다. 최신 느낌은 모양·색·타이포와 개별 애니메이션으로 낸다.
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = WordyShapes,
        typography = Typography,
        content = content,
    )
}

/**
 * 현재 테마에 맞는 숙달 단계 색.
 * 다크 모드에서 라이트용 색을 그대로 쓰면 어두운 바닥에 묻혀 탁해 보인다.
 */
val masteryColors: List<Color>
    @Composable @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MasteryColorsDark else MasteryColors
