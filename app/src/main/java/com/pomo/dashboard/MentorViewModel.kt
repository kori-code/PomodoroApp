// app/src/main/java/com/pomo/dashboard/MentorViewModel.kt
package com.pomo.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomo.auth.StudentConnection
import com.pomo.auth.User
import com.pomo.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MentorUiState(
    val mentorName: String = "",
    val mentorEmail: String = "",
    val mentorId: String = "",
    val totalStudents: Int = 0,
    val averageFocusScore: Int = 0,
    val totalSessions: Int = 0,
    val students: List<StudentConnection> = emptyList(),
    val isLoading: Boolean = false
)

class MentorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val userDao = database.userDao()
    private val studentDao = database.studentDao()
    private val sessionDao = database.sessionDao()
    
    private val _uiState = MutableStateFlow(MentorUiState())
    val uiState: StateFlow<MentorUiState> = _uiState.asStateFlow()
    
    fun loadMentorData(mentorId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val mentor = userDao.getUserById(mentorId)
            if (mentor != null) {
                _uiState.value = _uiState.value.copy(
                    mentorName = mentor.fullName,
                    mentorEmail = mentor.email,
                    mentorId = mentor.mentorId ?: "N/A",
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun loadConnectedStudents(mentorId: String) {
        viewModelScope.launch {
            val students = studentDao.getStudentsByMentor(mentorId)
            val mentor = userDao.getUserById(mentorId)
            
            var totalScore = 0
            var totalSessionsCount = 0
            
            students.forEach { student ->
                val sessions = sessionDao.getUserSessions(student.studentId)
                val completed = sessions.count { it.status == com.pomo.pomodoro.SessionStatus.COMPLETED }
                val total = sessions.size
                val score = if (total > 0) (completed * 100) / total else 0
                
                totalScore += score
                totalSessionsCount += completed
                
                // Update student stats
                studentDao.updateStudentStats(student.studentId, completed, score)
            }
            
            val avgScore = if (students.isNotEmpty()) totalScore / students.size else 0
            
            _uiState.value = _uiState.value.copy(
                students = students,
                totalStudents = students.size,
                averageFocusScore = avgScore,
                totalSessions = totalSessionsCount
            )
        }
    }
    
    fun logout() {
        // Handle logout
    }
}
