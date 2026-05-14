package com.pomo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomo.data.DailyStats
import com.pomo.data.PomodoroDatabase
import com.pomo.data.PomodoroRepository
import com.pomo.notification.NotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.random.Random

enum class TimerState { IDLE, RUNNING, PAUSED, FINISHED, FAILED, SKIPPED }

data class ProofChallenge(
    val question: String,
    val answer: Int,
    val attempts: Int = 3
)

data class PomodoroUiState(
    val timerState: TimerState = TimerState.IDLE,
    val secondsRemaining: Int = 1500,         // 25 minutes
    val currentSessionStart: Long? = null,
    val proofChallenge: ProofChallenge? = null,
    val proofChallengeRemainingAttempts: Int = 3,
    val proofChallengeResult: String? = null,  // "correct", "wrong", null
    val dailyStats: List<DailyStats> = emptyList(),
    val isServiceRunning: Boolean = false
)

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PomodoroDatabase.getInstance(application)
    private val repo = PomodoroRepository(db.pomodoroDao())

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var proofCheckJob: Job? = null

    // Count of proof checks this session
    private var proofChecksPassed = 0
    private var proofChecksTotal = 0

    // Track the last time a proof check was shown to avoid re-showing
    private var lastProofCheckSecond = 0

    init {
        // Create notification channels
        NotificationHelper.createChannels(application)

        // Load daily stats
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repo.getDailyStats().collect { stats ->
                _uiState.update { it.copy(dailyStats = stats) }
            }
        }
    }

    fun startSession() {
        val state = _uiState.value
        if (state.timerState == TimerState.RUNNING || state.timerState == TimerState.PAUSED) return

        _uiState.update {
            it.copy(
                timerState = TimerState.RUNNING,
                secondsRemaining = 1500,
                currentSessionStart = System.currentTimeMillis(),
                proofChallenge = null,
                proofChallengeResult = null,
                proofChallengeRemainingAttempts = 3
            )
        }

        proofChecksPassed = 0
        proofChecksTotal = 0
        lastProofCheckSecond = 0

        // Start foreground service for persistent timer notification
        startForegroundService()

        // Start the countdown
        timerJob = viewModelScope.launch(Dispatchers.IO) {
            val totalSeconds = 1500
            for (i in totalSeconds downTo 0) {
                if (!isActive) break

                val currentState = _uiState.value
                if (currentState.timerState == TimerState.FAILED ||
                    currentState.timerState == TimerState.SKIPPED) break

                // Update remaining time
                _uiState.update { it.copy(secondsRemaining = i) }

                // Update notification
                updateForegroundNotification(i, totalSeconds)

                // --- PROOF OF ACTIVITY CHECK every 5 minutes (300 seconds) ---
                val elapsedSeconds = totalSeconds - i
                val proofInterval = 300 // 5 minutes

                if (elapsedSeconds > 0 &&
                    elapsedSeconds % proofInterval == 0 &&
                    elapsedSeconds != lastProofCheckSecond &&
                    currentState.proofChallenge == null &&
                    currentState.timerState == TimerState.RUNNING
                ) {
                    lastProofCheckSecond = elapsedSeconds
                    proofChecksTotal++

                    // Pause timer, show challenge
                    _uiState.update {
                        it.copy(
                            timerState = TimerState.PAUSED,
                            proofChallenge = generateProofChallenge(),
                            proofChallengeRemainingAttempts = 3,
                            proofChallengeResult = null
                        )
                    }

                    // Wait for user to answer (will be resumed by submitProof)
                    // But set a timeout: if no answer in 30s, mark as failed
                    delay(30_000L)

                    // Check if still waiting for answer
                    val st = _uiState.value
                    if (st.proofChallenge != null && st.timerState != TimerState.FAILED) {
                        // Timeout — session FAILED
                        failSession("Proof-of-activity timeout after 30 seconds")
                        return@launch
                    }
                }

                delay(1000L)
            }

            // Timer reached zero naturally
            val finalState = _uiState.value
            if (finalState.timerState == TimerState.RUNNING && finalState.secondsRemaining == 0) {
                completeSession()
            }
        }
    }

    fun submitProof(answer: Int) {
        val state = _uiState.value
        val challenge = state.proofChallenge ?: return

        if (answer == challenge.answer) {
            // Correct!
            proofChecksPassed++
            _uiState.update {
                it.copy(
                    proofChallenge = null,
                    proofChallengeResult = "correct",
                    timerState = TimerState.RUNNING,
                    proofChallengeRemainingAttempts = 3
                )
            }
            // Resume the timer
        } else {
            val remaining = state.proofChallengeRemainingAttempts - 1
            if (remaining <= 0) {
                // All attempts exhausted — FAILED
                failSession("Proof-of-activity failed — all attempts used")
            } else {
                _uiState.update {
                    it.copy(
                        proofChallengeResult = "wrong",
                        proofChallengeRemainingAttempts = remaining
                    )
                }
            }
        }
    }

    fun skipSession() {
        if (_uiState.value.timerState != TimerState.RUNNING &&
            _uiState.value.timerState != TimerState.PAUSED) return

        val sessionStart = _uiState.value.currentSessionStart
        failSession("Session was manually skipped")

        // Mark as skipped separately so we can differentiate
        viewModelScope.launch {
            sessionStart?.let { start ->
                repo.recordSession(
                    startTimeMillis = start,
                    durationSeconds = 1500,
                    status = "SKIPPED",
                    proofChecksPassed = proofChecksPassed,
                    proofChecksTotal = proofChecksTotal
                )
            }
        }
    }

    private fun failSession(reason: String) {
        timerJob?.cancel()
        proofCheckJob?.cancel()

        val sessionStart = _uiState.value.currentSessionStart

        _uiState.update {
            it.copy(
                timerState = TimerState.FAILED,
                proofChallenge = null,
                secondsRemaining = 0
            )
        }

        stopForegroundService()

        // Record failure in DB
        viewModelScope.launch {
            sessionStart?.let { start ->
                repo.recordSession(
                    startTimeMillis = start,
                    durationSeconds = 1500,
                    status = "FAILED",
                    proofChecksPassed = proofChecksPassed,
                    proofChecksTotal = proofChecksTotal
                )
            }
        }

        // TRIGGER RED ALERT
        val app = getApplication<Application>()
        NotificationHelper.triggerRedAlert(app, reason)
    }

    private fun completeSession() {
        timerJob?.cancel()
        proofCheckJob?.cancel()

        val sessionStart = _uiState.value.currentSessionStart

        _uiState.update {
            it.copy(
                timerState = TimerState.FINISHED,
                proofChallenge = null,
                secondsRemaining = 0
            )
        }

        stopForegroundService()
        NotificationHelper.cancelTimerNotification(getApplication())

        // Record completion
        viewModelScope.launch {
            sessionStart?.let { start ->
                repo.recordSession(
                    startTimeMillis = start,
                    durationSeconds = 1500,
                    status = "COMPLETED",
                    proofChecksPassed = proofChecksPassed,
                    proofChecksTotal = max(proofChecksTotal, 1) // at least 1
                )
            }
        }
    }

    fun dismissAlert() {
        NotificationHelper.cancelRedAlert(getApplication())
        _uiState.update { it.copy(timerState = TimerState.IDLE) }
    }

    fun resetState() {
        timerJob?.cancel()
        proofCheckJob?.cancel()
        stopForegroundService()
        NotificationHelper.cancelTimerNotification(getApplication())
        _uiState.update {
            PomodoroUiState()
        }
    }

    private fun generateProofChallenge(): ProofChallenge {
        val rand = Random.Default
        return when (rand.nextInt(4)) {
            0 -> {
                val a = rand.nextInt(10, 100)
                val b = rand.nextInt(1, 50)
                ProofChallenge("$a + $b = ?", a + b)
            }
            1 -> {
                val a = rand.nextInt(20, 100)
                val b = rand.nextInt(1, 20)
                ProofChallenge("$a - $b = ?", a - b)
            }
            2 -> {
                val a = rand.nextInt(2, 15)
                val b = rand.nextInt(2, 12)
                ProofChallenge("$a × $b = ?", a * b)
            }
            3 -> {
                val a = rand.nextInt(1, 9)
                val numbers = List(a) { rand.nextInt(1, 100) }
                val sum = numbers.sum()
                ProofChallenge(
                    "Sum of ${numbers.joinToString(", ")} = ?",
                    sum
                )
            }
            else -> {
                val a = rand.nextInt(10, 100)
                val b = rand.nextInt(1, 50)
                ProofChallenge("$a + $b = ?", a + b)
            }
        }
    }

    private fun startForegroundService() {
        val context = getApplication<Application>()
        val intent = android.content.Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_TOTAL_SECONDS, 1500)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        _uiState.update { it.copy(isServiceRunning = true) }
    }

    private fun updateForegroundNotification(remaining: Int, total: Int) {
        val context = getApplication<Application>()
        val intent = android.content.Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_UPDATE
            putExtra(TimerService.EXTRA_REMAINING_SECONDS, remaining)
            putExtra(TimerService.EXTRA_TOTAL_SECONDS, total)
        }
        context.startService(intent)
    }

    private fun stopForegroundService() {
        val context = getApplication<Application>()
        val intent = android.content.Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        context.stopService(intent)
        _uiState.update { it.copy(isServiceRunning = false) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        proofCheckJob?.cancel()
    }
}
