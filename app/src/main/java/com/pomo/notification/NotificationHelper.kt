package com.pomo.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pomo.MainActivity

object NotificationHelper {

    const val CHANNEL_ID_RED_ALERT = "red_alert_channel"
    const val CHANNEL_ID_TIMER = "pomodoro_timer_channel"
    private const val NOTIFICATION_ID_TIMER = 1001
    private const val NOTIFICATION_ID_RED_ALERT = 1002

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val redAlertSound: Uri = try {
            Uri.parse("android.resource://${context.packageName}/raw/red_alert")
        } catch (_: Exception) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
        val alarmAttrs = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID_RED_ALERT, "Red Alert", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud alarm for skipped/cancelled sessions"
                setSound(redAlertSound, alarmAttrs); enableVibration(true)
                vibrationPattern = longArrayOf(0,500,300,500,300,1000)
                enableLights(true); setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            })
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID_TIMER, "Pomodoro Timer", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Timer progress"; setSound(null,null); enableVibration(false)
            })
        }
    }

    fun showTimerNotification(context: Context, secRemaining: Int, totalSec: Int) {
        val pi = PendingIntent.getActivity(context,0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val m = secRemaining/60; val s = secRemaining%60
        val pct = ((totalSec-secRemaining).toFloat()/totalSec*100).toInt()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TIMER,
            NotificationCompat.Builder(context, CHANNEL_ID_TIMER)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Pomodoro in Progress")
                .setContentText("$m:${String.format("%02d",s)} remaining")
                .setContentIntent(pi).setOngoing(true).setSilent(true)
                .setProgress(100,pct,false).setPriority(NotificationCompat.PRIORITY_LOW).build())
    }

    fun triggerRedAlert(context: Context, reason: String) {
        val pi = PendingIntent.getActivity(context,1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val di = PendingIntent.getBroadcast(context,2,
            Intent(context, RedAlertReceiver::class.java).apply {
                action = "com.pomo.DISMISS_RED_ALERT"
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val b = NotificationCompat.Builder(context, CHANNEL_ID_RED_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("\uD83D\uDD14 RED ALERT — Session Skipped!")
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$reason\n\nSession FAILED."))
            .setContentIntent(pi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Acknowledge", di)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(pi, true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            b.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            b.priority = NotificationCompat.PRIORITY_MAX
        }
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_RED_ALERT, b.build())
    }

    fun cancelTimerNotification(c: Context) { NotificationManagerCompat.from(c).cancel(NOTIFICATION_ID_TIMER) }
    fun cancelRedAlert(c: Context) { NotificationManagerCompat.from(c).cancel(NOTIFICATION_ID_RED_ALERT) }
}
