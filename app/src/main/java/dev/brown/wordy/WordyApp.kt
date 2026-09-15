package dev.brown.wordy

import android.app.Application
import dev.brown.wordy.di.AppContainer
import dev.brown.wordy.platform.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WordyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderScheduler.ensureChannel(this)
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            val s = container.settings.current()
            if (s.reminderEnabled) ReminderScheduler.schedule(this@WordyApp, s.reminderHour, s.reminderMinute)
        }
    }
}
