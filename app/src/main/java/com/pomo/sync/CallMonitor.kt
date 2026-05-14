package com.pomo.sync

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import com.pomo.data.MentorDetails
import com.pomo.data.MentorRepository

class CallMonitor(private val context: Context) {

    private var telephonyManager: TelephonyManager? = null
    private var lastCallNumber: String = ""
    private var callStartTime: Long = 0
    private var isInSession: Boolean = false
    private var onCallDetected: ((String, Int) -> Unit)? = null

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            if (!isInSession) return

            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    lastCallNumber = phoneNumber ?: "Unknown"
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    if (lastCallNumber.isNotBlank() && callStartTime == 0L) {
                        callStartTime = System.currentTimeMillis()
                        onCallDetected?.invoke(lastCallNumber, 0)
                    }
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    if (callStartTime > 0) {
                        val duration = ((System.currentTimeMillis() - callStartTime) / 1000).toInt()
                        onCallDetected?.invoke(lastCallNumber, duration)
                        callStartTime = 0
                        lastCallNumber = ""
                    }
                }
            }
        }
    }

    fun startMonitoring(onCall: (String, Int) -> Unit) {
        onCallDetected = onCall
        isInSession = true
        try {
            telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopMonitoring() {
        isInSession = false
        try {
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
