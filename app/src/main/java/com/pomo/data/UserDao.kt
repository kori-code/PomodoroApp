// app/src/main/java/com/pomo/data/UserDao.kt
package com.pomo.data

import androidx.room.*
import com.pomo.auth.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?
    
    @Query("SELECT * FROM users WHERE mentorId = :mentorId")
    suspend fun getUserByMentorId(mentorId: String): User?
    
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long
    
    @Query("UPDATE users SET isLoggedIn = :isLoggedIn WHERE id = :userId")
    suspend fun updateLoginStatus(userId: String, isLoggedIn: Boolean)
    
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>
}

// app/src/main/java/com/pomo/data/StudentDao.kt
package com.pomo.data

import androidx.room.*
import com.pomo.auth.StudentConnection

@Dao
interface StudentDao {
    @Query("SELECT * FROM student_connections WHERE mentorId = :mentorId")
    suspend fun getStudentsByMentor(mentorId: String): List<StudentConnection>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: StudentConnection)
    
    @Query("UPDATE student_connections SET totalSessionsCompleted = :completed, currentFocusScore = :score WHERE studentId = :studentId")
    suspend fun updateStudentStats(studentId: String, completed: Int, score: Int)
}

// app/src/main/java/com/pomo/data/SessionDao.kt
package com.pomo.data

import androidx.room.*
import com.pomo.auth.PomodoroSession
import com.pomo.pomodoro.SessionStatus

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: PomodoroSession)
    
    @Query("SELECT * FROM pomodoro_sessions WHERE userId = :userId ORDER BY startTime DESC")
    suspend fun getUserSessions(userId: String): List<PomodoroSession>
    
    @Query("SELECT * FROM pomodoro_sessions WHERE studentId = :studentId ORDER BY startTime DESC")
    suspend fun getStudentSessions(studentId: String): List<PomodoroSession>
}

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

// app/src/main/java/com/pomo/data/AppDatabase.kt
package com.pomo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pomo.auth.PomodoroSession
import com.pomo.auth.StudentConnection
import com.pomo.auth.User

@Database(
    entities = [User::class, StudentConnection::class, PomodoroSession::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun studentDao(): StudentDao
    abstract fun sessionDao(): SessionDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kori_pomodoro_v3.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
