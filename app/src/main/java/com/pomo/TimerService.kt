package com.pomo

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.pomo.notification.NotificationHelper
import kotlinx.coroutines.*

class TimerService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var timerJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer(intent.getIntExtra(EXTRA_TOTAL_SECONDS, 1500))
            ACTION_UPDATE -> NotificationHelper.showTimerNotification(this,
                intent.getIntExtra(EXTRA_REMAINING_SECONDS,0),
                intent.getIntExtra(EXTRA_TOTAL_SECONDS, 1500))
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startTimer(totalSeconds: Int) {
        timerJob?.cancel()
        timerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                NotificationHelper.showTimerNotification(this@TimerService, remaining, totalSeconds)
                delay(1000); remaining--
            }
            NotificationHelper.cancelTimerNotification(this@TimerService)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?) = null
    override fun onDestroy() { timerJob?.cancel(); scope.cancel(); super.onDestroy() }

    companion object {
        const val ACTION_START = "com.pomo.action.START_TIMER"
        const val ACTION_UPDATE = "com.pomo.action.UPDATE_TIMER"
        const val ACTION_STOP = "com.pomo.action.STOP_TIMER"
        const val EXTRA_TOTAL_SECONDS = "total_seconds"
        const val EXTRA_REMAINING_SECONDS = "remaining_seconds"
    }
}
