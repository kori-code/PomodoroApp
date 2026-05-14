package com.pomo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mentor_details")
data class MentorDetails(
    @PrimaryKey val id: Int = 1,  // singleton row
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val webhookUrl: String = "",  // optional custom endpoint
    val isSetupComplete: Boolean = false
)
