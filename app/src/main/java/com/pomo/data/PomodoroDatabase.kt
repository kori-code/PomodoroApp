package com.pomo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PomodoroSession::class, MentorDetails::class],
    version = 2,
    exportSchema = false
)
abstract class PomodoroDatabase : RoomDatabase() {
    abstract fun pomodoroDao(): PomodoroDao
    abstract fun mentorDao(): MentorDao

    companion object {
        @Volatile
        private var INSTANCE: PomodoroDatabase? = null

        fun getInstance(context: Context): PomodoroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PomodoroDatabase::class.java,
                    "kori_pomodoro.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
