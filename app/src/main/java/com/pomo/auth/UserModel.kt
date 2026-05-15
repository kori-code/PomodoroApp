package com.pomo.auth

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val role: UserRole,
    val fullName: String,
    val dateOfBirth: String,
    val phoneNumber: String,
    val email: String,
    val password: String,  // NEW: Add password field
    val mentorId: String? = null,
    val connectedMentorId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isLoggedIn: Boolean = false
)

enum class UserRole {
    MENTOR, STUDENT
}

@Entity(tableName = "student_connections")
data class StudentConnection(
    @PrimaryKey
    val studentId: String,
    val mentorId: String,
    val studentName: String,
    val studentPhone: String,
    val studentEmail: String,
    val lastActive: Long = System.currentTimeMillis(),
    val totalSessionsCompleted: Int = 0,
    val currentFocusScore: Int = 0
)

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val studentId: String? = null,
    val taskName: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Int,
    val status: SessionStatus,
    val proofChecksPassed: Int,
    val proofChecksTotal: Int
)

enum class SessionStatus {
    COMPLETED, FAILED, SKIPPED, INTERRUPTED
}
