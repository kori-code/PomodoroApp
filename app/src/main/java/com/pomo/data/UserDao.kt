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
    
    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getLoggedInUser(): User?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long
    
    @Query("UPDATE users SET isLoggedIn = :isLoggedIn WHERE id = :userId")
    suspend fun updateLoginStatus(userId: String, isLoggedIn: Boolean)
    
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>
}
