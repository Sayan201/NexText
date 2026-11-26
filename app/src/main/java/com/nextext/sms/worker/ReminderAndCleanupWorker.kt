package com.nextext.sms.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nextext.sms.NexTextApplication
import com.nextext.sms.sms.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ReminderAndCleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NexTextApplication ?: return Result.success()
        val container = app.container

        val prefs = container.preferencesRepository.preferences.first()
        if (prefs.autoDeleteOldOtps) {
            container.smsRepository.cleanOldOtps(maxAgeHours = 24L)
        }

        val now = System.currentTimeMillis()
        val windowEnd = now + TimeUnit.HOURS.toMillis(24)
        val dueReminders = container.database.reminderDao().getUpcomingDueReminders(now, windowEnd)
        for (reminder in dueReminders) {
            NotificationHelper.showReminderNotification(
                context = applicationContext,
                reminderId = reminder.id,
                title = reminder.title,
                description = reminder.description
            )
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "nextext_periodic_maintenance"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderAndCleanupWorker>(6, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NexTextApplication ?: return Result.success()
        val backupDir = java.io.File(applicationContext.filesDir, "auto_backups").apply { mkdirs() }
        val file = java.io.File(backupDir, "nextext-autobackup.json")
        app.container.backupManager.exportToUri(android.net.Uri.fromFile(file))
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "nextext_auto_backup"

        fun schedule(context: Context, frequency: com.nextext.sms.data.preferences.AutoBackupFrequency) {
            val wm = WorkManager.getInstance(context)
            if (frequency == com.nextext.sms.data.preferences.AutoBackupFrequency.OFF) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                frequency.intervalDays,
                TimeUnit.DAYS
            ).build()
            wm.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
