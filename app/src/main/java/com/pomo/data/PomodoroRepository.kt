package com.pomo.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PomodoroRepository(private val dao: PomodoroDao) {

    fun getSessionsByDate(date: String): Flow<List<PomodoroSession>> =
        dao.getSessionsByDate(date)

    fun getDailyStats(): Flow<List<DailyStats>> =
        dao.getDailyStats()

    suspend fun recordSession(
        startTimeMillis: Long,
        durationSeconds: Int,
        status: String,
        proofChecksPassed: Int,
        proofChecksTotal: Int
    ) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(startTimeMillis))
        val session = PomodoroSession(
            date = date,
            startTimeMillis = startTimeMillis,
            durationSeconds = durationSeconds,
            status = status,
            proofChecksPassed = proofChecksPassed,
            proofChecksTotal = proofChecksTotal
        )
        dao.insert(session)
    }

    suspend fun cleanupOldSessions() {
        val thirtyDaysAgo = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
            Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
        )
        dao.deleteOldSessions(thirtyDaysAgo)
    }
}
