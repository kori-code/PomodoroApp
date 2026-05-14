package com.pomo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MentorDao {

    @Query("SELECT * FROM mentor_details WHERE id = 1")
    suspend fun getMentor(): MentorDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMentor(mentor: MentorDetails)

    @Query("DELETE FROM mentor_details")
    suspend fun clearMentor()
}
