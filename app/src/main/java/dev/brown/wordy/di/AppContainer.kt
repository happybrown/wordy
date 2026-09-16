package dev.brown.wordy.di

import android.content.Context
import dev.brown.wordy.data.db.content.ContentDatabase
import dev.brown.wordy.data.db.progress.ProgressDatabase
import dev.brown.wordy.data.prefs.SettingsRepository
import dev.brown.wordy.data.repo.ProgressRepository
import dev.brown.wordy.data.repo.WordRepository
import dev.brown.wordy.domain.session.ExerciseGenerator
import dev.brown.wordy.domain.session.LearningEngine
import dev.brown.wordy.domain.session.SessionBuilder
import dev.brown.wordy.platform.BackupManager
import dev.brown.wordy.platform.TtsManager

/** 수동 DI 컨테이너. Application 이 하나 만들어 들고 있고, ViewModel 들은 여기서 의존성을 받는다. */
class AppContainer(private val context: Context) {
    val contentVersion: String by lazy { ContentDatabase.assetVersion(context) ?: "no-content" }
    val contentDb: ContentDatabase by lazy { ContentDatabase.open(context) }
    val progressDb: ProgressDatabase by lazy { ProgressDatabase.open(context) }

    val settings: SettingsRepository by lazy { SettingsRepository(context) }
    val words: WordRepository by lazy { WordRepository(contentDb.dao()) }
    val progress: ProgressRepository by lazy { ProgressRepository(progressDb) }
    val engine: LearningEngine by lazy { LearningEngine(words, progress, settings) }
    val sessionBuilder: SessionBuilder by lazy { SessionBuilder(words, progress, settings) }
    val exercises: ExerciseGenerator by lazy { ExerciseGenerator(words) }
    val tts: TtsManager by lazy { TtsManager(context) }
    val backup: BackupManager by lazy { BackupManager(progressDb) }

    fun now(): Long = System.currentTimeMillis()
}
