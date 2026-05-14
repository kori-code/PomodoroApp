package com.pomo.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RedAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.pomo.DISMISS_RED_ALERT") {
            NotificationHelper.cancelRedAlert(context)
        }
    }
}
