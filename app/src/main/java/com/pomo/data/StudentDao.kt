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
    
    @Query("DELETE FROM student_connections WHERE studentId = :studentId")
    suspend fun deleteConnection(studentId: String)
}
