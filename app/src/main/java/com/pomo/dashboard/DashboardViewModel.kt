package com.pomo.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomo.auth.PomodoroSession
import com.pomo.auth.StudentConnection
import com.pomo.auth.User
import com.pomo.data.AppDatabase
import com.pomo.pomodoro.PomodoroEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class DashboardUiState(
    val userFullName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val userRole: String = "",
    val mentorId: String = "",
    val timerState: PomodoroEngine.TimerState = PomodoroEngine.TimerState.IDLE,
    val secondsRemaining: Int = 25 * 60,
    val currentTask: String = "",
    val focusScore: Int = 0,
    val totalCompleted: Int = 0,
    val streakDays: Int = 0,
    val level: String = "Beginner",
    val isLoading: Boolean = false,
    val currentSessionId: Long = 0
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val userDao = database.userDao()
    private val sessionDao = database.sessionDao()
    private val studentDao = database.studentDao()
    
    private val pomodoroEngine = PomodoroEngine(application)
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    val proofChallengeFlow = pomodoroEngine.onProofChallenge
    
    private var currentUserId: String = ""
    private var currentUser: User? = null
    private var currentSessionId: Long = 0
    
    fun initializePomodoro() {
        viewModelScope.launch {
            pomodoroEngine.uiState.collect { engineState ->
                _uiState.value = _uiState.value.copy(
                    timerState = engineState.state,
                    secondsRemaining = engineState.secondsRemaining,
                    currentTask = engineState.currentTask
                )
                
                // Update session end time when completed or failed
                if (engineState.state == PomodoroEngine.TimerState.COMPLETED && _uiState.value.currentSessionId != 0L) {
                    updateSessionStatus("COMPLETED", engineState.proofChecksPassed, engineState.proofChecksTotal)
                } else if (engineState.state == PomodoroEngine.TimerState.FAILED && _uiState.value.currentSessionId != 0L) {
                    updateSessionStatus("FAILED", engineState.proofChecksPassed, engineState.proofChecksTotal)
                }
            }
        }
    }
    
    fun loadUserData(userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            currentUser = userDao.getUserById(userId)
            if (currentUser != null) {
                val sessions = sessionDao.getUserSessions(userId)
                val completed = sessions.count { it.status == "COMPLETED" }
                val total = sessions.size
                val score = if (total > 0) (completed * 100) / total else 0
                val streak = calculateStreak(sessions)
                val level = getLevel(completed)
                
                _uiState.value = _uiState.value.copy(
                    userFullName = currentUser!!.fullName,
                    userEmail = currentUser!!.email,
                    userPhone = currentUser!!.phoneNumber,
                    userRole = currentUser!!.role.name,
                    mentorId = currentUser!!.mentorId ?: "",
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
            val session = PomodoroSession(
                userId = currentUserId,
                studentId = if (_uiState.value.userRole == "STUDENT") currentUserId else null,
                taskName = taskName,
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis() + (25 * 60 * 1000),
                durationSeconds = 25 * 60,
                status = "STARTED",
                proofChecksPassed = 0,
                proofChecksTotal = 0
            )
            currentSessionId = sessionDao.insertSession(session)
            _uiState.value = _uiState.value.copy(currentSessionId = currentSessionId)
        }
    }
    
    private fun updateSessionStatus(status: String, proofPassed: Int, proofTotal: Int) {
        viewModelScope.launch {
            if (_uiState.value.currentSessionId != 0L) {
                // Update the session with final status
                val session = PomodoroSession(
                    id = _uiState.value.currentSessionId,
                    userId = currentUserId,
                    studentId = if (_uiState.value.userRole == "STUDENT") currentUserId else null,
                    taskName = _uiState.value.currentTask,
                    startTime = System.currentTimeMillis() - (25 * 60 * 1000),
                    endTime = System.currentTimeMillis(),
                    durationSeconds = 25 * 60,
                    status = status,
                    proofChecksPassed = proofPassed,
                    proofChecksTotal = proofTotal
                )
                sessionDao.insertSession(session)
                
                // Refresh user data to update stats
                loadUserData(currentUserId)
                
                // If student, notify mentor via refresh
                if (_uiState.value.userRole == "STUDENT" && currentUser?.connectedMentorId != null) {
                    // The mentor dashboard will auto-refresh and pick up this session
                }
            }
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
        _uiState.value = _uiState.value.copy(currentSessionId = 0)
    }
    
    fun logout() {
        viewModelScope.launch {
            userDao.updateLoginStatus(currentUserId, false)
        }
    }
    
    fun refreshStudentData() {
        loadUserData(currentUserId)
    }
    
    private fun calculateStreak(sessions: List<PomodoroSession>): Int {
        val completedDates = sessions
            .filter { it.status == "COMPLETED" }
            .map { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.startTime)) }
            .distinct()
            .sortedDescending()
        
        var streak = 0
        for (i in completedDates.indices) {
            if (i == 0) streak = 1
            else {
                val prevDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(completedDates[i - 1])
                val currDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(completedDates[i])
                val diff = ((prevDate?.time ?: 0) - (currDate?.time ?: 0)) / (24 * 60 * 60 * 1000)
                if (diff == 1L) streak++ else break
            }
        }
        return streak
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
