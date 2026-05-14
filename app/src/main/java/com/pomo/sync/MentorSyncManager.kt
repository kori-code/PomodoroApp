package com.pomo.sync

import android.content.Context
import com.google.gson.Gson
import com.pomo.data.MentorDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class SessionUpdate(
    val type: String,                // "STARTED", "CHALLENGE", "COMPLETED", "FAILED", "SKIPPED", "CALL_RECEIVED"
    val studentName: String = "Student",
    val taskName: String = "",
    val timestamp: String = "",
    val elapsedMinutes: Int = 0,
    val proofChecksPassed: Int = 0,
    val proofChecksTotal: Int = 0,
    val callFrom: String = "",
    val callDuration: Int = 0,
    val message: String = ""
)

object MentorSyncManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun notifyMentor(
        context: Context,
        mentor: MentorDetails?,
        update: SessionUpdate
    ) {
        if (mentor == null || !mentor.isSetupComplete) return

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .format(Date())

        val fullUpdate = update.copy(timestamp = timestamp)

        // 1. Send to webhook if configured
        if (mentor.webhookUrl.isNotBlank()) {
            sendWebhook(mentor.webhookUrl, fullUpdate)
        }

        // 2. Send SMS if phone is configured
        if (mentor.phone.isNotBlank()) {
            sendSms(context, mentor.phone, formatSmsMessage(fullUpdate))
        }

        // 3. Send email via intent (opens email app)
        if (mentor.email.isNotBlank()) {
            sendEmailIntent(context, mentor.email, fullUpdate)
        }
    }

    private suspend fun sendWebhook(url: String, update: SessionUpdate) {
        withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(update)
                val body = json.toRequestBody(JSON)
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendSms(context: Context, phone: String, message: String) {
        try {
            val smsManager = android.telephony.SmsManager.getDefault()
            smsManager.sendTextMessage(phone, null, message, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendEmailIntent(context: Context, email: String, update: SessionUpdate) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Kori Pomodoro Update: ${update.type}")
            putExtra(android.content.Intent.EXTRA_TEXT, formatEmailMessage(update))
        }
        context.startActivity(
            android.content.Intent.createChooser(intent, "Send update to mentor")
        )
    }

    private fun formatSmsMessage(update: SessionUpdate): String {
        return when (update.type) {
            "STARTED" -> "🍅 Kori Pomodoro STARTED!\nTask: ${update.taskName}\nTime: ${update.timestamp}"
            "COMPLETED" -> "✅ Pomodoro COMPLETED!\nTask: ${update.taskName}\nChecks: ${update.proofChecksPassed}/${update.proofChecksTotal}\nTime: ${update.timestamp}"
            "FAILED" -> "❌ Pomodoro FAILED!\nTask: ${update.taskName}\nReason: ${update.message}\nTime: ${update.timestamp}"
            "SKIPPED" -> "⚠️ Pomodoro SKIPPED!\nTime: ${update.timestamp}"
            "CALL_RECEIVED" -> "📞 Call received from ${update.callFrom} during focus session!\nDuration: ${update.callDuration}s\nTime: ${update.timestamp}"
            "CHALLENGE" -> "🧠 Proof check ${update.proofChecksPassed}/${update.proofChecksTotal} passed\nTime: ${update.timestamp}"
            else -> "Kori Pomodoro update: ${update.message}\nTime: ${update.timestamp}"
        }
    }

    private fun formatEmailMessage(update: SessionUpdate): String {
        return """
            |🍅 KORI POMODORO UPDATE
            |═══════════════════════
            |Type: ${update.type}
            |Task: ${update.taskName}
            |Time: ${update.timestamp}
            |Elapsed: ${update.elapsedMinutes} minutes
            |Proof Checks: ${update.proofChecksPassed}/${update.proofChecksTotal}
            |${if (update.callFrom.isNotBlank()) "Call From: ${update.callFrom}" else ""}
            |${if (update.callDuration > 0) "Call Duration: ${update.callDuration}s" else ""}
            |Message: ${update.message}
            |
            |Stay focused! 💪
        """.trimMargin()
    }
}
