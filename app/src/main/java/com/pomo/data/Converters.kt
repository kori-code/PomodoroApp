package com.pomo.data

import androidx.room.TypeConverter

class Converters {
    
    @TypeConverter
    fun fromUserRole(role: String): String = role
    
    @TypeConverter
    fun toUserRole(role: String): String = role
}
