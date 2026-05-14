package com.pomo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val startTimeMillis: Long,
    val durationSeconds: Int,
    val status: String,
    val proofChecksPassed: Int,
    val proofChecksTotal: Int
)
