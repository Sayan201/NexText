package com.nextext.sms.worker

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nextext.sms.NexTextApplication
import com.nextext.sms.data.backup.DriveFolderBackup
import com.nextext.sms.data.preferences.AutoBackupFrequency
import com.nextext.sms.sms.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
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
        val prefs = app.container.preferencesRepository.preferences.first()

        // Legacy builds wrote a local snapshot here; the Drive folder is the only
        // destination now, so drop the leftover file rather than let it stack.
        java.io.File(applicationContext.filesDir, "auto_backups").deleteRecursively()

        if (prefs.autoBackupFrequency == AutoBackupFrequency.OFF) return Result.success()

        val tree = prefs.driveBackupTreeUri?.let(Uri::parse) ?: return Result.success()

        val bytes = app.container.backupManager.exportToBytes().getOrElse {
            return Result.retry()
        }

        val outcome = withContext(Dispatchers.IO) {
            DriveFolderBackup.write(applicationContext, tree, bytes)
        }

        return outcome.fold(
            onSuccess = {
                app.container.preferencesRepository
                    .setLastDriveBackupAt(System.currentTimeMillis())
                Result.success()
            },
            onFailure = {
                if (!DriveFolderBackup.hasPersistedWrite(applicationContext, tree)) {
                    app.container.preferencesRepository.setDriveBackupTreeUri(null)
                    app.container.preferencesRepository.setDriveBackupFolderName(null)
                }
                Result.retry()
            }
        )
    }

    companion object {
        private const val WORK_NAME = "nextext_auto_backup"
        private const val ONCE_WORK_NAME = "nextext_auto_backup_once"

        /** Fires one immediate backup, e.g. right after a folder is linked. */
        fun runOnce(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONCE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<AutoBackupWorker>()
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                    .build()
            )
        }

        fun schedule(context: Context, frequency: AutoBackupFrequency) {
            val wm = WorkManager.getInstance(context)
            if (frequency == AutoBackupFrequency.OFF) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                frequency.intervalDays,
                TimeUnit.DAYS
            )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
