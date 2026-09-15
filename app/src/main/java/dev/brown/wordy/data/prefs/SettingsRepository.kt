package dev.brown.wordy.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.brown.wordy.domain.model.Level
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Settings(
    val onboardingDone: Boolean = false,
    val currentLevel: Level = Level.ELEM,
    val dailyNewWords: Int = 10,
    val coreEnabled: Boolean = true,
    /** 신규 단어 중 Core 트랙 비율 (0.0~1.0) */
    val coreRatio: Float = 0.4f,
    val desiredRetention: Float = 0.9f,
    val ttsRate: Float = 0.95f,
    val strictTyping: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
    val aiAssistEnabled: Boolean = false,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val currentLevel = stringPreferencesKey("current_level")
        val dailyNewWords = intPreferencesKey("daily_new_words")
        val coreEnabled = booleanPreferencesKey("core_enabled")
        val coreRatio = floatPreferencesKey("core_ratio")
        val desiredRetention = floatPreferencesKey("desired_retention")
        val ttsRate = floatPreferencesKey("tts_rate")
        val strictTyping = booleanPreferencesKey("strict_typing")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val aiAssistEnabled = booleanPreferencesKey("ai_assist_enabled")
    }

    val settings: Flow<Settings> = context.settingsStore.data.map { p ->
        Settings(
            onboardingDone = p[Keys.onboardingDone] ?: false,
            currentLevel = p[Keys.currentLevel]?.let(Level::of) ?: Level.ELEM,
            dailyNewWords = p[Keys.dailyNewWords] ?: 10,
            coreEnabled = p[Keys.coreEnabled] ?: true,
            coreRatio = p[Keys.coreRatio] ?: 0.4f,
            desiredRetention = p[Keys.desiredRetention] ?: 0.9f,
            ttsRate = p[Keys.ttsRate] ?: 0.95f,
            strictTyping = p[Keys.strictTyping] ?: false,
            reminderEnabled = p[Keys.reminderEnabled] ?: false,
            reminderHour = p[Keys.reminderHour] ?: 21,
            reminderMinute = p[Keys.reminderMinute] ?: 0,
            aiAssistEnabled = p[Keys.aiAssistEnabled] ?: false,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun update(block: (Settings) -> Settings) {
        val next = block(current())
        context.settingsStore.edit { p ->
            p[Keys.onboardingDone] = next.onboardingDone
            p[Keys.currentLevel] = next.currentLevel.code
            p[Keys.dailyNewWords] = next.dailyNewWords.coerceIn(0, 50)
            p[Keys.coreEnabled] = next.coreEnabled
            p[Keys.coreRatio] = next.coreRatio.coerceIn(0f, 1f)
            p[Keys.desiredRetention] = next.desiredRetention.coerceIn(0.75f, 0.97f)
            p[Keys.ttsRate] = next.ttsRate.coerceIn(0.5f, 1.5f)
            p[Keys.strictTyping] = next.strictTyping
            p[Keys.reminderEnabled] = next.reminderEnabled
            p[Keys.reminderHour] = next.reminderHour.coerceIn(0, 23)
            p[Keys.reminderMinute] = next.reminderMinute.coerceIn(0, 59)
            p[Keys.aiAssistEnabled] = next.aiAssistEnabled
        }
    }
}
