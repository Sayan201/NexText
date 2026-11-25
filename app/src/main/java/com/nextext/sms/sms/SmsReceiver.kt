package com.nextext.sms.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SubscriptionManager
import com.nextext.sms.NexTextApplication
import com.nextext.sms.domain.model.MessageCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val app = context.applicationContext as? NexTextApplication ?: return
        val repo = app.container.smsRepository
        val isDefault = repo.isDefaultSmsApp()

        // When default SMS app, Android sends SMS_DELIVER; ignore SMS_RECEIVED to avoid double ingestion.
        if (action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION && isDefault) return
        if (action != Telephony.Sms.Intents.SMS_DELIVER_ACTION &&
            action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        ) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val subId = intent.getIntExtra(
            SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
            intent.getIntExtra("subscription", -1)
        )

        val grouped = messages.groupBy { it.originatingAddress ?: "Unknown" }
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = app.container.preferencesRepository.preferences.first()
                for ((address, parts) in grouped) {
                    val fullBody = parts.joinToString(separator = "") { it.messageBody ?: "" }
                    val timestamp = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
                    if (fullBody.isBlank()) continue

                    val saved = repo.ingestIncomingSms(
                        address = address,
                        body = fullBody,
                        timestamp = timestamp,
                        subId = subId
                    )

                    val shouldNotify = saved.category != MessageCategory.SPAM &&
                        !(saved.category == MessageCategory.PROMOTIONS && prefs.mutePromotionsNotifications)

                    if (shouldNotify) {
                        val otpForNotif = if (prefs.showOtpCopyNotification) saved.otpCode else null
                        NotificationHelper.showIncomingSmsNotification(
                            context = context,
                            threadId = saved.threadId,
                            senderTitle = saved.contactName ?: saved.address,
                            body = saved.body,
                            category = saved.category,
                            otpCode = otpForNotif,
                            smartBadge = saved.smartBadge
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
