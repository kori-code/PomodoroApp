package com.pomo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,          // ISO date: "2026-05-14"
    val startTimeMillis: Long, // epoch millis when session started
    val durationSeconds: Int,  // planned duration (1500 = 25 min)
    val status: String,        // "COMPLETED", "FAILED", "SKIPPED"
    val proofChecksPassed: Int, // how many proof-of-activity checks were answered
    val proofChecksTotal: Int   // total checks that appeared
)
