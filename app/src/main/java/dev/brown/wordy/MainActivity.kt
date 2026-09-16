package dev.brown.wordy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.brown.wordy.ui.WordyApp
import dev.brown.wordy.ui.theme.WordyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 앱이 항상 밝은 화면이므로 시스템 바 아이콘도 어두운 색으로 고정한다.
        // 인자 없이 enableEdgeToEdge() 를 부르면 기기 다크 모드를 따라가
        // 흰 배경 위에 흰 글자가 얹혀 시간·배터리가 보이지 않는다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(TRANSPARENT, TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val container = (application as WordyApp).container
        setContent {
            WordyTheme {
                WordyApp(container)
            }
        }
    }

    private companion object {
        const val TRANSPARENT = android.graphics.Color.TRANSPARENT
    }
}
