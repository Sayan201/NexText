package com.nextext.sms.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Required Default SMS App component for WAP_PUSH_DELIVER
    }
}
