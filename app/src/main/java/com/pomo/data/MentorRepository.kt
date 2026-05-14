package com.pomo.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MentorRepository(private val mentorDao: MentorDao) {

    suspend fun getMentor(): MentorDetails? = mentorDao.getMentor()

    suspend fun saveMentor(mentor: MentorDetails) {
        mentorDao.saveMentor(mentor.copy(id = 1, isSetupComplete = true))
    }

    suspend fun isSetupComplete(): Boolean {
        return mentorDao.getMentor()?.isSetupComplete ?: false
    }

    suspend fun clearMentor() = mentorDao.clearMentor()
}
