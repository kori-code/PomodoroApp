package com.pomo.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
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

    /**
     * Create notification channels.
     * Must be called once in Application.onCreate() or MainActivity.onCreate().
     */
    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ------------------------------------------------------------------
        // CHANNEL 1: RED ALERT — highest priority, custom loud sound
        // ------------------------------------------------------------------
        val redAlertSound: Uri = Uri.parse(
            "android.resource://${context.packageName}/raw/red_alert"
        )

        val redAlertAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()

        val redAlertChannel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNEL_ID_RED_ALERT,
                "Red Alert — Skipped Sessions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud alarm for skipped or cancelled Pomodoro sessions"
                setSound(redAlertSound, redAlertAttrs)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500, 300, 1000)
                enableLights(true)
                // Bypass Do Not Disturb so it always rings
                setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
        } else {
            // Pre-Oreo, sound is configured on the notification itself
            null
        }

        // ------------------------------------------------------------------
        // CHANNEL 2: Timer foreground service (lower priority, no sound)
        // ------------------------------------------------------------------
        val timerChannel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNEL_ID_TIMER,
                "Pomodoro Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows current Pomodoro timer progress"
                setSound(null, null)
                enableVibration(false)
            }
        } else {
            null
        }

        // Register channels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            redAlertChannel?.let { manager.createNotificationChannel(it) }
            timerChannel?.let { manager.createNotificationChannel(it) }
        }
    }

    /**
     * Show the persistent timer notification for the foreground service.
     */
    fun showTimerNotification(
        context: Context,
        secondsRemaining: Int,
        totalSeconds: Int
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val minutes = secondsRemaining / 60
        val seconds = secondsRemaining % 60
        val progress = ((totalSeconds - secondsRemaining).toFloat() / totalSeconds * 100).toInt()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Pomodoro in Progress")
            .setContentText("${minutes}:${String.format("%02d", seconds)} remaining")
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setProgress(100, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TIMER, notification)
    }

    /**
     * THE RED ALERT — trigger when a session is skipped or cancelled.
     * Uses the custom loud sound channel, repeats, and requires explicit dismiss.
     */
    fun triggerRedAlert(context: Context, reason: String) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action — marks the alert as acknowledged
        val dismissIntent = Intent(context, RedAlertReceiver::class.java).apply {
            action = "com.pomo.DISMISS_RED_ALERT"
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, 2, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_RED_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("\uD83D\uDD14 RED ALERT — Session Skipped!")
            .setContentText(reason)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$reason\n\nThis session has been marked as FAILED. Tap to view your stats.")
            )
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Acknowledge",
                dismissPendingIntent
            )
            .setAutoCancel(false)           // Must be manually dismissed
            .setOngoing(true)               // Stays until acknowledged
            .setFullScreenIntent(openPendingIntent, true) // Show on locked screen
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Pre-Oreo: set sound directly
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notification.setSound(
                Uri.parse("android.resource://${context.packageName}/raw/red_alert"),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
            notification.priority = NotificationCompat.PRIORITY_MAX
        }

        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID_RED_ALERT,
            notification.build()
        )
    }

    fun cancelTimerNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_TIMER)
    }

    fun cancelRedAlert(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_RED_ALERT)
    }
}
