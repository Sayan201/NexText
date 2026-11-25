package com.nextext.sms.sms

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.telephony.TelephonyManager
import com.nextext.sms.NexTextApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == TelephonyManager.ACTION_RESPOND_VIA_MESSAGE) {
            val extras = intent.extras
            val message = extras?.getCharSequence(Intent.EXTRA_TEXT)?.toString().orEmpty()
            val recipient = extractRecipient(intent.data)
            val app = applicationContext as? NexTextApplication
            if (app != null && recipient.isNotBlank() && message.isNotBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    app.container.smsRepository.sendSms(recipient, message)
                    stopSelf(startId)
                }
                return START_NOT_STICKY
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun extractRecipient(uri: Uri?): String {
        val raw = uri?.schemeSpecificPart ?: return ""
        return raw.substringBefore('?').trim()
    }
}
