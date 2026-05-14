package com.pomo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroDao {

    @Insert
    suspend fun insert(session: PomodoroSession)

    @Query("SELECT * FROM pomodoro_sessions WHERE date = :date ORDER BY startTimeMillis DESC")
    fun getSessionsByDate(date: String): Flow<List<PomodoroSession>>

    @Query("""
        SELECT date,
               SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed,
               SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed,
               SUM(CASE WHEN status = 'SKIPPED' THEN 1 ELSE 0 END) AS skipped,
               COUNT(*) AS total
        FROM pomodoro_sessions
        GROUP BY date
        ORDER BY date DESC
        LIMIT 30
    """)
    fun getDailyStats(): Flow<List<DailyStats>>

    @Query("DELETE FROM pomodoro_sessions WHERE date < :beforeDate")
    suspend fun deleteOldSessions(beforeDate: String)
}

data class DailyStats(
    val date: String,
    val completed: Int,
    val failed: Int,
    val skipped: Int,
    val total: Int
)
