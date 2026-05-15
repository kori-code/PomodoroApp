package com.pomo.pomodoro

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.random.Random

enum class SessionStatus {
    COMPLETED, FAILED, SKIPPED
}

class PomodoroEngine(private val application: Application) : ViewModel() {
    
    enum class TimerState { IDLE, RUNNING, PAUSED, COMPLETED, FAILED }
    
    data class ProofChallenge(
        val question: String,
        val answer: Int,
        val attemptsLeft: Int = 3
    )
    
    data class TimerStateData(
        val state: TimerState = TimerState.IDLE,
        val secondsRemaining: Int = 25 * 60,
        val currentTask: String = "",
        val sessionStartTime: Long = 0,
        val proofChecksPassed: Int = 0,
        val proofChecksTotal: Int = 0
    )
    
    private val _uiState = MutableStateFlow(TimerStateData())
    val uiState: StateFlow<TimerStateData> = _uiState
    
    private val _proofChallenge = MutableSharedFlow<ProofChallenge>()
    val onProofChallenge = _proofChallenge.asSharedFlow()
    
    private var timerJob: Job? = null
    private var lastProofCheckSecond = 0
    private var currentChallengeAnswer: Int = 0
    
    fun startSession(taskName: String) {
        if (_uiState.value.state != TimerState.IDLE) return
        
        _uiState.value = TimerStateData(
            state = TimerState.RUNNING,
            secondsRemaining = 25 * 60,
            currentTask = taskName,
            sessionStartTime = System.currentTimeMillis(),
            proofChecksPassed = 0,
            proofChecksTotal = 0
        )
        
        lastProofCheckSecond = 0
        startTimer()
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch(Dispatchers.IO) {
            val totalSeconds = _uiState.value.secondsRemaining
            for (i in totalSeconds downTo 0) {
                if (!isActive) break
                if (_uiState.value.state != TimerState.RUNNING) break
                
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(secondsRemaining = i)
                }
                
                val elapsed = totalSeconds - i
                if (elapsed > 0 && elapsed % 300 == 0 && elapsed != lastProofCheckSecond) {
                    lastProofCheckSecond = elapsed
                    triggerProofCheck()
                    
                    delay(30000L)
                    if (_uiState.value.state == TimerState.PAUSED) {
                        failSession("Proof challenge timeout")
                        return@launch
                    }
                }
                
                delay(1000L)
            }
            
            if (_uiState.value.state == TimerState.RUNNING && _uiState.value.secondsRemaining == 0) {
                completeSession()
            }
        }
    }
    
    private suspend fun triggerProofCheck() {
        val challenge = generateProofChallenge()
        currentChallengeAnswer = challenge.answer
        _proofChallenge.emit(challenge)
        _uiState.value = _uiState.value.copy(
            state = TimerState.PAUSED,
            proofChecksTotal = _uiState.value.proofChecksTotal + 1
        )
    }
    
    fun submitProofAnswer(answer: Int) {
        val currentState = _uiState.value
        if (currentState.state != TimerState.PAUSED) return
        
        if (answer == currentChallengeAnswer) {
            _uiState.value = currentState.copy(
                state = TimerState.RUNNING,
                proofChecksPassed = currentState.proofChecksPassed + 1
            )
        }
    }
    
    fun pauseTimer() {
        if (_uiState.value.state == TimerState.RUNNING) {
            _uiState.value = _uiState.value.copy(state = TimerState.PAUSED)
            timerJob?.cancel()
        }
    }
    
    fun resumeTimer() {
        if (_uiState.value.state == TimerState.PAUSED) {
            _uiState.value = _uiState.value.copy(state = TimerState.RUNNING)
            startTimer()
        }
    }
    
    fun failSession(reason: String) {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(state = TimerState.FAILED)
        saveSession(SessionStatus.FAILED)
    }
    
    private fun completeSession() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(state = TimerState.COMPLETED)
        saveSession(SessionStatus.COMPLETED)
    }
    
    private fun saveSession(status: SessionStatus) {
    }
    
    fun resetTimer() {
        timerJob?.cancel()
        _uiState.value = TimerStateData()
    }
    
    private fun generateProofChallenge(): ProofChallenge {
        val random = Random.Default
        return when (random.nextInt(5)) {
            0 -> {
                val a = random.nextInt(10, 100)
                val b = random.nextInt(1, 50)
                ProofChallenge("$a + $b = ?", a + b)
            }
            1 -> {
                val a = random.nextInt(20, 100)
                val b = random.nextInt(1, 20)
                ProofChallenge("$a - $b = ?", a - b)
            }
            2 -> {
                val a = random.nextInt(2, 15)
                val b = random.nextInt(2, 12)
                ProofChallenge("$a × $b = ?", a * b)
            }
            3 -> {
                val a = random.nextInt(5, 20)
                val b = random.nextInt(1, 10)
                ProofChallenge("$a ÷ $b = ? (round down)", a / b)
            }
            else -> {
                val numbers = List(random.nextInt(2, 6)) { random.nextInt(1, 50) }
                ProofChallenge("Sum of ${numbers.joinToString(", ")} = ?", numbers.sum())
            }
        }
    }
}
