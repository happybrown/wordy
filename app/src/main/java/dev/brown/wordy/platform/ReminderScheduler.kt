package dev.brown.wordy.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.brown.wordy.MainActivity
import dev.brown.wordy.R
import dev.brown.wordy.WordyApp
import dev.brown.wordy.data.repo.ProgressRepository
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** 하루 한 번(설정 시각) 복습 알림. Worker 가 실행 후 다음 날로 스스로 재예약한다. */
object ReminderScheduler {
    const val CHANNEL_ID = "daily_reminder"
    private const val WORK_NAME = "wordy-daily-reminder"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "복습 알림", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "하루 한 번 예정된 복습을 알려 줍니다" })
        }
    }

    fun schedule(context: Context, hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var next = now.with(LocalTime.of(hour, minute, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) { WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME) }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as WordyApp
        val c = app.container
        val settings = c.settings.current()
        if (!settings.reminderEnabled) return Result.success()
        val now = c.now()
        val due = c.progress.dueCount(now).first()
        val today = c.progress.observeDaily(ProgressRepository.today(now)).first()
        val studiedToday = (today?.reviewed ?: 0) + (today?.newLearned ?: 0) > 0
        if ((due > 0 || !studiedToday) && ReminderScheduler.hasPermission(applicationContext)) {
            ReminderScheduler.ensureChannel(applicationContext)
            val intent = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val text = when {
                due > 0 -> "복습 ${due}개가 기다리고 있어요. 5분이면 충분해요."
                else -> "오늘 아직 새 단어를 만나지 않았어요. ${settings.dailyNewWords}개만 볼까요?"
            }
            val n = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle("Wordy").setContentText(text)
                .setContentIntent(intent).setAutoCancel(true).build()
            applicationContext.getSystemService(NotificationManager::class.java).notify(1001, n)
        }
        ReminderScheduler.schedule(applicationContext, settings.reminderHour, settings.reminderMinute)
        return Result.success()
    }
}
