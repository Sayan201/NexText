package com.nextext.sms.sms

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.nextext.sms.NexTextApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)
        val threadId = intent.getLongExtra(NotificationHelper.EXTRA_THREAD_ID, -1L)

        when (intent.action) {
            NotificationHelper.ACTION_COPY_OTP -> {
                val otp = intent.getStringExtra(NotificationHelper.EXTRA_OTP_CODE)
                if (!otp.isNullOrBlank()) {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("OTP", otp))
                    Toast.makeText(context, "OTP $otp copied", Toast.LENGTH_SHORT).show()
                }
                dismissAndMarkRead(context, notificationId, threadId)
            }

            NotificationHelper.ACTION_MARK_READ -> {
                dismissAndMarkRead(context, notificationId, threadId)
            }

            NotificationHelper.ACTION_DELETE -> {
                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }
                if (threadId != -1L) {
                    val app = context.applicationContext as? NexTextApplication ?: return
                    val pending = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            app.container.smsRepository.deleteThread(threadId)
                        } finally {
                            pending.finish()
                        }
                    }
                }
            }

            NotificationHelper.ACTION_REPLY -> {
                val replyText = RemoteInput.getResultsFromIntent(intent)
                    ?.getCharSequence(NotificationHelper.KEY_TEXT_REPLY)
                    ?.toString()
                    ?.trim()
                val address = intent.getStringExtra(NotificationHelper.EXTRA_SENDER_ADDRESS).orEmpty()

                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }

                if (!replyText.isNullOrBlank() && address.isNotBlank()) {
                    val app = context.applicationContext as? NexTextApplication ?: return
                    val pending = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            if (threadId != -1L) {
                                app.container.smsRepository.markThreadRead(threadId)
                            }
                            app.container.smsRepository.sendSms(
                                address = address,
                                body = replyText,
                                subId = -1,
                                existingThreadId = if (threadId > 0L) threadId else null
                            )
                        } finally {
                            pending.finish()
                        }
                    }
                    Toast.makeText(context, "Reply sent", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun dismissAndMarkRead(context: Context, notificationId: Int, threadId: Long) {
        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
        if (threadId != -1L) {
            val app = context.applicationContext as? NexTextApplication ?: return
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    app.container.smsRepository.markThreadRead(threadId)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
