package com.pomo.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PomodoroRepository(private val dao: PomodoroDao) {

    fun getDailyStats(): Flow<List<DailyStats>> = dao.getDailyStats()

    suspend fun recordSession(
        startTimeMillis: Long,
        durationSeconds: Int,
        status: String,
        proofChecksPassed: Int,
        proofChecksTotal: Int
    ) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(startTimeMillis))
        dao.insert(
            PomodoroSession(
                date = date,
                startTimeMillis = startTimeMillis,
                durationSeconds = durationSeconds,
                status = status,
                proofChecksPassed = proofChecksPassed,
                proofChecksTotal = proofChecksTotal
            )
        )
    }
}
