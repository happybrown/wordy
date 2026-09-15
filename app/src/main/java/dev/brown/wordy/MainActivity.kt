package dev.brown.wordy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.brown.wordy.ui.WordyApp
import dev.brown.wordy.ui.theme.WordyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as WordyApp).container
        setContent {
            WordyTheme {
                WordyApp(container)
            }
        }
    }
}
