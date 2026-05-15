// app/src/main/java/com/pomo/data/Converters.kt
package com.pomo.data

import androidx.room.TypeConverter
import com.pomo.auth.SessionStatus
import com.pomo.auth.UserRole

class Converters {
    
    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name
    
    @TypeConverter
    fun toUserRole(role: String): UserRole = UserRole.valueOf(role)
    
    @TypeConverter
    fun fromSessionStatus(status: SessionStatus): String = status.name
    
    @TypeConverter
    fun toSessionStatus(status: String): SessionStatus = SessionStatus.valueOf(status)
}
