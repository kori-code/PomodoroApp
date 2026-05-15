package com.pomo.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomo.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentUser: User? = null,
    val errorMessage: String? = null,
    val fullName: String = "",
    val dateOfBirth: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val mentorId: String = "",
    val mentorPhone: String = ""
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val userDao = database.userDao()
    private val studentDao = database.studentDao()
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState
    
    fun updateFullName(value: String) { _uiState.value = _uiState.value.copy(fullName = value) }
    fun updateDateOfBirth(value: String) { _uiState.value = _uiState.value.copy(dateOfBirth = value) }
    fun updatePhoneNumber(value: String) { _uiState.value = _uiState.value.copy(phoneNumber = value) }
    fun updateEmail(value: String) { _uiState.value = _uiState.value.copy(email = value) }
    fun updateMentorId(value: String) { _uiState.value = _uiState.value.copy(mentorId = value) }
    fun updateMentorPhone(value: String) { _uiState.value = _uiState.value.copy(mentorPhone = value) }
    
    fun signUp(
        role: UserRole,
        fullName: String,
        dateOfBirth: String,
        phoneNumber: String,
        email: String,
        mentorId: String?,
        mentorPhone: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val generatedMentorId = if (role == UserRole.MENTOR) {
                    generateMentorId()
                } else null
                
                val user = User(
                    role = role,
                    fullName = fullName,
                    dateOfBirth = dateOfBirth,
                    phoneNumber = phoneNumber,
                    email = email,
                    mentorId = generatedMentorId,
                    connectedMentorId = if (role == UserRole.STUDENT) mentorId else null,
                    isLoggedIn = true
                )
                
                val existingUser = userDao.getUserByEmail(email)
                if (existingUser != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "User with this email already exists. Please login."
                    )
                    return@launch
                }
                
                userDao.insertUser(user)
                
                if (role == UserRole.STUDENT && !mentorId.isNullOrEmpty() && !mentorPhone.isNullOrEmpty()) {
                    val mentor = userDao.getUserByMentorId(mentorId)
                    if (mentor != null) {
                        if (mentor.phoneNumber == mentorPhone) {
                            val connection = StudentConnection(
                                studentId = user.id,
                                mentorId = mentorId,
                                studentName = fullName,
                                studentPhone = phoneNumber,
                                studentEmail = email
                            )
                            studentDao.insertConnection(connection)
                        } else {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Mentor phone number verification failed"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Invalid Mentor ID. Please check with your mentor."
                        )
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    currentUser = user
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Sign up failed: ${e.message}"
                )
            }
        }
    }
    
    fun login(email: String, phoneNumber: String, role: UserRole) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val user = userDao.getUserByEmail(email)
                
                if (user == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "User not found. Please sign up first."
                    )
                    return@launch
                }
                
                if (user.phoneNumber != phoneNumber) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Invalid phone number for this account"
                    )
                    return@launch
                }
                
                if (user.role != role) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Account exists but with different role"
                    )
                    return@launch
                }
                
                userDao.updateLoginStatus(user.id, true)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    currentUser = user.copy(isLoggedIn = true)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Login failed: ${e.message}"
                )
            }
        }
    }
    
    private fun generateMentorId(): String {
        return String.format("%06d", Random.nextInt(100000, 999999))
    }
}
