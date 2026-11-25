package com.nextext.sms.sms

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
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
