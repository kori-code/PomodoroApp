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
    val password: String = "",
    val mentorId: String = "",
    val mentorPhone: String = ""
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val userDao = database.userDao()
    private val studentDao = database.studentDao()
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState
    
    init {
        viewModelScope.launch {
            val loggedInUser = userDao.getLoggedInUser()
            if (loggedInUser != null) {
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = true,
                    currentUser = loggedInUser
                )
            }
        }
    }
    
    fun updateFullName(value: String) { _uiState.value = _uiState.value.copy(fullName = value) }
    fun updateDateOfBirth(value: String) { _uiState.value = _uiState.value.copy(dateOfBirth = value) }
    fun updatePhoneNumber(value: String) { _uiState.value = _uiState.value.copy(phoneNumber = value) }
    fun updateEmail(value: String) { _uiState.value = _uiState.value.copy(email = value) }
    fun updatePassword(value: String) { _uiState.value = _uiState.value.copy(password = value) }
    fun updateMentorId(value: String) { _uiState.value = _uiState.value.copy(mentorId = value) }
    fun updateMentorPhone(value: String) { _uiState.value = _uiState.value.copy(mentorPhone = value) }
    
    fun signUp(
        role: UserRole,
        fullName: String,
        dateOfBirth: String,
        phoneNumber: String,
        email: String,
        password: String,
        mentorId: String?,
        mentorPhone: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                if (fullName.isBlank() || email.isBlank() || phoneNumber.isBlank() || password.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Please fill all required fields"
                    )
                    return@launch
                }
                
                if (password.length < 4) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Password must be at least 4 characters"
                    )
                    return@launch
                }
                
                val existingUser = userDao.getUserByEmail(email)
                if (existingUser != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "User with this email already exists. Please login."
                    )
                    return@launch
                }
                
                val generatedMentorId = if (role == UserRole.MENTOR) {
                    generateMentorId()
                } else null
                
                val user = User(
                    role = role,
                    fullName = fullName,
                    dateOfBirth = dateOfBirth,
                    phoneNumber = phoneNumber,
                    email = email,
                    password = password,
                    mentorId = generatedMentorId,
                    connectedMentorId = if (role == UserRole.STUDENT) mentorId else null,
                    isLoggedIn = true
                )
                
                userDao.insertUser(user)
                
                // STUDENT: Connect to mentor
                if (role == UserRole.STUDENT && !mentorId.isNullOrEmpty()) {
                    // Find the mentor by mentorId
                    val mentor = userDao.getUserByMentorId(mentorId)
                    
                    if (mentor != null) {
                        // Verify mentor phone if provided
                        if (mentorPhone.isNullOrEmpty() || mentor.phoneNumber == mentorPhone) {
                            // Check if connection already exists
                            val existingConnections = studentDao.getStudentsByMentor(mentorId)
                            val alreadyConnected = existingConnections.any { it.studentId == user.id }
                            
                            if (!alreadyConnected) {
                                val connection = StudentConnection(
                                    studentId = user.id,
                                    mentorId = mentorId,
                                    studentName = fullName,
                                    studentPhone = phoneNumber,
                                    studentEmail = email,
                                    lastActive = System.currentTimeMillis(),
                                    totalSessionsCompleted = 0,
                                    currentFocusScore = 0
                                )
                                studentDao.insertConnection(connection)
                                _uiState.value = _uiState.value.copy(
                                    errorMessage = "Successfully connected to mentor: ${mentor.fullName}"
                                )
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    errorMessage = "Already connected to this mentor"
                                )
                            }
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
    
    fun login(email: String, phoneNumber: String, password: String, role: UserRole) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                if (email.isBlank() || password.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Please enter email and password"
                    )
                    return@launch
                }
                
                val user = userDao.getUserByEmail(email)
                
                if (user == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "User not found. Please sign up first."
                    )
                    return@launch
                }
                
                if (user.password != password) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Invalid password"
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
    
    fun logout() {
        viewModelScope.launch {
            _uiState.value.currentUser?.let {
                userDao.updateLoginStatus(it.id, false)
            }
        }
        _uiState.value = AuthUiState()
    }
    
    private fun generateMentorId(): String {
        return String.format("%06d", Random.nextInt(100000, 999999))
    }
}
