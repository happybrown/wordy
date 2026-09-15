package dev.brown.wordy.platform

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/** 안드로이드 내장 TTS(en-US) 래퍼. 오프라인에서도 동작하며, 엔진이 없으면 available=false 로 조용히 비활성화된다. */
class TtsManager(context: Context) {
    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available
    var rate: Float = 0.95f

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val result = ttsRef?.setLanguage(Locale.US)
            _available.value = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }
    private val ttsRef: TextToSpeech? get() = tts

    fun speak(text: String, slow: Boolean = false) {
        if (!_available.value) return
        tts.setSpeechRate(if (slow) rate * 0.7f else rate)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "wordy-${System.nanoTime()}")
    }

    fun stop() { if (_available.value) tts.stop() }
    fun shutdown() { tts.shutdown() }
}
