package com.nextext.sms.sms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.nextext.sms.MainActivity
import com.nextext.sms.R
import com.nextext.sms.domain.model.MessageCategory

object NotificationHelper {

    const val CHANNEL_MESSAGES = "nextext_messages"
    const val CHANNEL_OTP = "nextext_otp"
    const val CHANNEL_TRANSACTIONS = "nextext_transactions"
    const val CHANNEL_REMINDERS = "nextext_reminders"

    const val ACTION_COPY_OTP = "com.nextext.sms.ACTION_COPY_OTP"
    const val ACTION_MARK_READ = "com.nextext.sms.ACTION_MARK_READ"
    const val ACTION_REPLY = "com.nextext.sms.ACTION_REPLY"
    const val ACTION_DELETE = "com.nextext.sms.ACTION_DELETE"
    const val KEY_TEXT_REPLY = "key_text_reply"
    const val EXTRA_OTP_CODE = "extra_otp_code"
    const val EXTRA_THREAD_ID = "extra_thread_id"
    const val EXTRA_SENDER_ADDRESS = "extra_sender_address"
    const val EXTRA_SENDER_TITLE = "extra_sender_title"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            NotificationChannel(
                CHANNEL_MESSAGES,
                context.getString(R.string.channel_messages_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_messages_desc)
            },
            NotificationChannel(
                CHANNEL_OTP,
                context.getString(R.string.channel_otp_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_otp_desc)
            },
            NotificationChannel(
                CHANNEL_TRANSACTIONS,
                context.getString(R.string.channel_transactions_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_transactions_desc)
            },
            NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_reminders_desc)
            }
        )
        manager.createNotificationChannels(channels)
    }

    fun showIncomingSmsNotification(
        context: Context,
        threadId: Long,
        senderTitle: String,
        address: String = senderTitle,
        body: String,
        category: MessageCategory,
        otpCode: String?,
        smartBadge: String?
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val notificationId = (threadId.toInt() and 0x7FFFFF) + 1000
        val channelId = when {
            otpCode != null -> CHANNEL_OTP
            category == MessageCategory.TRANSACTIONS -> CHANNEL_TRANSACTIONS
            else -> CHANNEL_MESSAGES
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(EXTRA_SENDER_TITLE, senderTitle)
            putExtra(EXTRA_SENDER_ADDRESS, address)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (otpCode != null) "OTP: $otpCode • $senderTitle" else senderTitle
        val content = if (!smartBadge.isNullOrBlank() && otpCode == null) "$smartBadge\n$body" else body

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(
                if (otpCode != null || category == MessageCategory.PERSONAL) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)

        if (otpCode != null) {
            val copyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_COPY_OTP
                putExtra(EXTRA_OTP_CODE, otpCode)
                putExtra(EXTRA_THREAD_ID, threadId)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            val copyPending = PendingIntent.getBroadcast(
                context,
                notificationId + 1,
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Copy $otpCode", copyPending)
        }

        // Inline Reply action (RemoteInput requires FLAG_MUTABLE on Android 12+)
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply")
            .build()
        val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(EXTRA_SENDER_ADDRESS, address)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val replyFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val replyPending = PendingIntent.getBroadcast(
            context,
            notificationId + 3,
            replyIntent,
            replyFlags
        )
        builder.addAction(
            NotificationCompat.Action.Builder(0, "Reply", replyPending)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build()
        )

        // Delete action
        val deleteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DELETE
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val deletePending = PendingIntent.getBroadcast(
            context,
            notificationId + 4,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, "Delete", deletePending)

        // Mark Read action
        val markReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_READ
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markReadPending = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, "Mark Read", markReadPending)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    fun showReminderNotification(
        context: Context,
        reminderId: Long,
        title: String,
        description: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(description))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        NotificationManagerCompat.from(context).notify(50_000 + reminderId.toInt(), notification)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
