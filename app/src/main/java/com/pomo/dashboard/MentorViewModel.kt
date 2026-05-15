package com.pomo.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomo.auth.StudentConnection
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
    
    private var currentMentorId: String = ""
    
    fun loadMentorData(mentorId: String) {
        currentMentorId = mentorId
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
                // Load students immediately
                loadConnectedStudents(mentorId)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun loadConnectedStudents(mentorId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Get all students connected to this mentor
            val students = studentDao.getStudentsByMentor(mentorId)
            
            var totalScore = 0
            var totalSessionsCount = 0
            val updatedStudents = mutableListOf<StudentConnection>()
            
            for (student in students) {
                // Get all sessions for this student
                val sessions = sessionDao.getUserSessions(student.studentId)
                val completed = sessions.count { it.status == "COMPLETED" }
                val total = sessions.size
                val score = if (total > 0) (completed * 100) / total else 0
                
                totalScore += score
                totalSessionsCount += completed
                
                // Update student stats in database
                studentDao.updateStudentStats(student.studentId, completed, score)
                
                // Create updated student connection with latest stats
                val updatedStudent = student.copy(
                    totalSessionsCompleted = completed,
                    currentFocusScore = score,
                    lastActive = sessions.maxOfOrNull { it.endTime } ?: student.lastActive
                )
                updatedStudents.add(updatedStudent)
            }
            
            // Sort students by focus score (highest first)
            val sortedStudents = updatedStudents.sortedByDescending { it.currentFocusScore }
            
            val avgScore = if (sortedStudents.isNotEmpty()) totalScore / sortedStudents.size else 0
            
            _uiState.value = _uiState.value.copy(
                students = sortedStudents,
                totalStudents = sortedStudents.size,
                averageFocusScore = avgScore,
                totalSessions = totalSessionsCount,
                isLoading = false
            )
        }
    }
    
    fun refreshData() {
        if (currentMentorId.isNotBlank()) {
            loadConnectedStudents(currentMentorId)
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            userDao.updateLoginStatus(currentMentorId, false)
        }
    }
}
