// app/src/main/java/com/pomo/data/SessionDao.kt
package com.pomo.data

import androidx.room.*
import com.pomo.auth.PomodoroSession

@Dao
interface SessionDao {
    
    @Insert
    suspend fun insertSession(session: PomodoroSession)
    
    @Query("SELECT * FROM pomodoro_sessions WHERE userId = :userId ORDER BY startTime DESC")
    suspend fun getUserSessions(userId: String): List<PomodoroSession>
    
    @Query("SELECT * FROM pomodoro_sessions WHERE studentId = :studentId ORDER BY startTime DESC")
    suspend fun getStudentSessions(studentId: String): List<PomodoroSession>
}
