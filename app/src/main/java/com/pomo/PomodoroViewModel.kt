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
data class ProofChallenge(val question: String, val answer: Int, val attempts: Int = 3)
data class PomodoroUiState(
    val timerState: TimerState = TimerState.IDLE,
    val secondsRemaining: Int = 1500,
    val currentSessionStart: Long? = null,
    val proofChallenge: ProofChallenge? = null,
    val proofChallengeRemainingAttempts: Int = 3,
    val proofChallengeResult: String? = null,
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
    private var proofChecksPassed = 0
    private var proofChecksTotal = 0
    private var lastProofCheckSecond = 0

    init {
        NotificationHelper.createChannels(application)
        viewModelScope.launch {
            repo.getDailyStats().collect { stats -> _uiState.update { it.copy(dailyStats = stats) } }
        }
    }

    fun startSession() {
        if (_uiState.value.timerState == TimerState.RUNNING || _uiState.value.timerState == TimerState.PAUSED) return
        _uiState.update { it.copy(timerState = TimerState.RUNNING, secondsRemaining = 1500,
            currentSessionStart = System.currentTimeMillis(), proofChallenge = null,
            proofChallengeResult = null, proofChallengeRemainingAttempts = 3) }
        proofChecksPassed = 0; proofChecksTotal = 0; lastProofCheckSecond = 0
        startForegroundService()

        timerJob = viewModelScope.launch(Dispatchers.IO) {
            val total = 1500
            for (i in total downTo 0) {
                if (!isActive) break
                val s = _uiState.value
                if (s.timerState == TimerState.FAILED || s.timerState == TimerState.SKIPPED) break
                _uiState.update { it.copy(secondsRemaining = i) }
                updateForegroundNotification(i, total)
                val elapsed = total - i
                if (elapsed > 0 && elapsed % 300 == 0 && elapsed != lastProofCheckSecond
                    && s.proofChallenge == null && s.timerState == TimerState.RUNNING) {
                    lastProofCheckSecond = elapsed; proofChecksTotal++
                    _uiState.update { it.copy(timerState = TimerState.PAUSED,
                        proofChallenge = generateProofChallenge(),
                        proofChallengeRemainingAttempts = 3, proofChallengeResult = null) }
                    delay(30_000L)
                    if (_uiState.value.proofChallenge != null && _uiState.value.timerState != TimerState.FAILED) {
                        failSession("Proof-of-activity timeout after 30 seconds"); return@launch
                    }
                }
                delay(1000L)
            }
            if (_uiState.value.timerState == TimerState.RUNNING && _uiState.value.secondsRemaining == 0)
                completeSession()
        }
    }

    fun submitProof(answer: Int) {
        val ch = _uiState.value.proofChallenge ?: return
        if (answer == ch.answer) {
            proofChecksPassed++
            _uiState.update { it.copy(proofChallenge = null, proofChallengeResult = "correct",
                timerState = TimerState.RUNNING, proofChallengeRemainingAttempts = 3) }
        } else {
            val rem = _uiState.value.proofChallengeRemainingAttempts - 1
            if (rem <= 0) failSession("Proof-of-activity failed — all attempts used")
            else _uiState.update { it.copy(proofChallengeResult = "wrong",
                proofChallengeRemainingAttempts = rem) }
        }
    }

    fun skipSession() {
        if (_uiState.value.timerState != TimerState.RUNNING && _uiState.value.timerState != TimerState.PAUSED) return
        val start = _uiState.value.currentSessionStart
        failSession("Session was manually skipped")
        viewModelScope.launch {
            start?.let { repo.recordSession(it, 1500, "SKIPPED", proofChecksPassed, proofChecksTotal) }
        }
    }

    private fun failSession(reason: String) {
        timerJob?.cancel(); proofCheckJob?.cancel()
        val start = _uiState.value.currentSessionStart
        _uiState.update { it.copy(timerState = TimerState.FAILED, proofChallenge = null, secondsRemaining = 0) }
        stopForegroundService()
        viewModelScope.launch { start?.let { repo.recordSession(it,1500,"FAILED",proofChecksPassed,proofChecksTotal) } }
        NotificationHelper.triggerRedAlert(getApplication(), reason)
    }

    private fun completeSession() {
        timerJob?.cancel(); proofCheckJob?.cancel()
        val start = _uiState.value.currentSessionStart
        _uiState.update { it.copy(timerState = TimerState.FINISHED, proofChallenge = null, secondsRemaining = 0) }
        stopForegroundService(); NotificationHelper.cancelTimerNotification(getApplication())
        viewModelScope.launch { start?.let { repo.recordSession(it,1500,"COMPLETED",proofChecksPassed,max(proofChecksTotal,1)) } }
    }

    fun dismissAlert() { NotificationHelper.cancelRedAlert(getApplication()); _uiState.update { it.copy(timerState = TimerState.IDLE) } }
    fun resetState() { timerJob?.cancel(); proofCheckJob?.cancel(); stopForegroundService()
        NotificationHelper.cancelTimerNotification(getApplication()); _uiState.value = PomodoroUiState() }

    private fun generateProofChallenge(): ProofChallenge {
        val r = Random.Default
        return when (r.nextInt(4)) {
            0 -> { val a=r.nextInt(10,100); val b=r.nextInt(1,50); ProofChallenge("$a + $b = ?", a+b) }
            1 -> { val a=r.nextInt(20,100); val b=r.nextInt(1,20); ProofChallenge("$a - $b = ?", a-b) }
            2 -> { val a=r.nextInt(2,15); val b=r.nextInt(2,12); ProofChallenge("$a × $b = ?", a*b) }
            3 -> { val n=List(r.nextInt(1,9)){r.nextInt(1,100)}; ProofChallenge("Sum of ${n.joinToString(", ")} = ?", n.sum()) }
            else -> { val a=r.nextInt(10,100); val b=r.nextInt(1,50); ProofChallenge("$a + $b = ?", a+b) }
        }
    }

    private fun startForegroundService() {
        getApplication<Application>().startForegroundService(
            android.content.Intent(getApplication(), TimerService::class.java).apply {
                action = TimerService.ACTION_START; putExtra(TimerService.EXTRA_TOTAL_SECONDS, 1500) })
        _uiState.update { it.copy(isServiceRunning = true) }
    }
    private fun updateForegroundNotification(rem: Int, tot: Int) {
        getApplication<Application>().startService(
            android.content.Intent(getApplication(), TimerService::class.java).apply {
                action = TimerService.ACTION_UPDATE
                putExtra(TimerService.EXTRA_REMAINING_SECONDS, rem)
                putExtra(TimerService.EXTRA_TOTAL_SECONDS, tot) })
    }
    private fun stopForegroundService() {
        getApplication<Application>().stopService(
            android.content.Intent(getApplication(), TimerService::class.java).apply {
                action = TimerService.ACTION_STOP })
        _uiState.update { it.copy(isServiceRunning = false) }
    }
    override fun onCleared() { timerJob?.cancel(); proofCheckJob?.cancel(); super.onCleared() }
}
