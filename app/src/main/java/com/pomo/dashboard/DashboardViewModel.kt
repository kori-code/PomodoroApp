// app/src/main/java/com/pomo/dashboard/DashboardViewModel.kt
package com.pomo.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomo.auth.User
import com.pomo.data.AppDatabase
import com.pomo.pomodoro.PomodoroEngine
import com.pomo.pomodoro.SessionStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val userFullName: String = "",
    val userEmail: String = "",
    val timerState: PomodoroEngine.TimerState = PomodoroEngine.TimerState.IDLE,
    val secondsRemaining: Int = 25 * 60,
    val currentTask: String = "",
    val focusScore: Int = 0,
    val totalCompleted: Int = 0,
    val streakDays: Int = 0,
    val level: String = "Beginner",
    val isLoading: Boolean = false
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val userDao = database.userDao()
    private val sessionDao = database.sessionDao()
    
    private val pomodoroEngine = PomodoroEngine(application)
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    val proofChallengeFlow = pomodoroEngine.onProofChallenge
    
    private var currentUserId: String = ""
    
    fun initializePomodoro() {
        viewModelScope.launch {
            pomodoroEngine.uiState.collect { engineState ->
                _uiState.update { current ->
                    current.copy(
                        timerState = engineState.state,
                        secondsRemaining = engineState.secondsRemaining,
                        currentTask = engineState.currentTask
                    )
                }
            }
        }
    }
    
    fun loadUserData(userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val user = userDao.getUserById(userId)
            if (user != null) {
                val sessions = sessionDao.getUserSessions(userId)
                val completed = sessions.count { it.status == SessionStatus.COMPLETED }
                val total = sessions.size
                val score = if (total > 0) (completed * 100) / total else 0
                val streak = calculateStreak(sessions)
                val level = getLevel(completed)
                
                _uiState.value = _uiState.value.copy(
                    userFullName = user.fullName,
                    userEmail = user.email,
                    focusScore = score,
                    totalCompleted = completed,
                    streakDays = streak,
                    level = level,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun startSession(taskName: String) {
        pomodoroEngine.startSession(taskName)
        viewModelScope.launch {
            // Save session start to database
        }
    }
    
    fun pauseTimer() {
        pomodoroEngine.pauseTimer()
    }
    
    fun resumeTimer() {
        pomodoroEngine.resumeTimer()
    }
    
    fun submitProofAnswer(answer: Int) {
        pomodoroEngine.submitProofAnswer(answer)
    }
    
    fun failSession(reason: String) {
        pomodoroEngine.failSession(reason)
    }
    
    fun resetTimer() {
        pomodoroEngine.resetTimer()
    }
    
    fun logout() {
        viewModelScope.launch {
            userDao.updateLoginStatus(currentUserId, false)
        }
    }
    
    private fun calculateStreak(sessions: List<com.pomo.auth.PomodoroSession>): Int {
        // Group by date and calculate streak
        return 0 // Simplified for now
    }
    
    private fun getLevel(completed: Int): String = when {
        completed >= 500 -> "💎 Diamond"
        completed >= 200 -> "🏆 Platinum"
        completed >= 100 -> "🥇 Gold"
        completed >= 50 -> "🥈 Silver"
        completed >= 20 -> "🥉 Bronze"
        completed >= 5 -> "⭐ Rookie"
        else -> "🌱 Beginner"
    }
}
