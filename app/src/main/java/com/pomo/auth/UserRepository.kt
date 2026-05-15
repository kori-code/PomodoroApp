// app/src/main/java/com/pomo/auth/UserRepository.kt
package com.pomo.auth

import android.content.Context
import com.pomo.data.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UserRepository(private val context: Context) {
    
    private val database = AppDatabase.getInstance(context)
    private val userDao = database.userDao()
    private val studentDao = database.studentDao()
    
    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }
    
    suspend fun getUserByMentorId(mentorId: String): User? {
        return userDao.getUserByMentorId(mentorId)
    }
    
    suspend fun insertUser(user: User): Long {
        return userDao.insertUser(user)
    }
    
    suspend fun updateLoginStatus(userId: String, isLoggedIn: Boolean) {
        userDao.updateLoginStatus(userId, isLoggedIn)
    }
    
    suspend fun getConnectedStudents(mentorId: String): List<StudentConnection> {
        return studentDao.getStudentsByMentor(mentorId)
    }
    
    suspend fun insertStudentConnection(connection: StudentConnection) {
        studentDao.insertConnection(connection)
    }
    
    suspend fun updateStudentStats(studentId: String, completed: Int, score: Int) {
        studentDao.updateStudentStats(studentId, completed, score)
    }
    
    fun getAllUsers(): Flow<List<User>> {
        return flow {
            emit(userDao.getAllUsers())
        }
    }
}
