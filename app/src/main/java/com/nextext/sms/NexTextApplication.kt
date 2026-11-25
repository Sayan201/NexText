package com.nextext.sms

import android.app.Application
import android.content.Context
import com.nextext.sms.data.backup.BackupManager
import com.nextext.sms.data.local.NexTextDatabase
import com.nextext.sms.data.preferences.UserPreferencesRepository
import com.nextext.sms.data.repository.SmsRepository
import com.nextext.sms.sms.NotificationHelper
import com.nextext.sms.worker.ReminderAndCleanupWorker

class AppContainer(context: Context) {
    val database: NexTextDatabase = NexTextDatabase.getInstance(context)
    val preferencesRepository: UserPreferencesRepository = UserPreferencesRepository(context)
    val smsRepository: SmsRepository = SmsRepository(context, database, preferencesRepository)
    val backupManager: BackupManager = BackupManager(context, database)
}

class NexTextApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannels(this)
        ReminderAndCleanupWorker.schedule(this)
    }
}
