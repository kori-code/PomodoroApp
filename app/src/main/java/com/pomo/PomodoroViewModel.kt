package com.pomo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomo.data.*
import com.pomo.notification.NotificationHelper
import com.pomo.sync.CallMonitor
import com.pomo.sync.MentorSyncManager
import com.pomo.sync.SessionUpdate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.max
import kotlin.random.Random

enum class TimerState { IDLE, RUNNING, PAUSED, FINISHED, FAILED, SKIPPED }
data class ProofChallenge(val question: String, val answer: Int, val attempts: Int = 3)

data class PomodoroUiState(
    val timerState: TimerState = TimerState.IDLE,
    val secondsRemaining: Int = 1500,
    val currentSessionStart: Long? = null,
    val currentTaskName: String = "",
    val proofChallenge: ProofChallenge? = null,
    val proofChallengeRemainingAttempts: Int = 3,
    val proofChallengeResult: String? = null,
    val dailyStats: List<DailyStats> = emptyList(),
    val isServiceRunning: Boolean = false,
    val mentorSetupComplete: Boolean = false,
    val mentor: MentorDetails? = null,
    val showTaskDialog: Boolean = false,

    // Addictive features
    val streakDays: Int = 0,
    val totalCompletedSessions: Int = 0,
    val focusScore: Int = 0,       // 0-100
    val level: String = "Beginner", // Beginner, Bronze, Silver, Gold, Platinum, Diamond
    val motivationQuote: String = ""
)

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PomodoroDatabase.getInstance(application)
    private val repo = PomodoroRepository(db.pomodoroDao())
    private val mentorRepo = MentorRepository(db.mentorDao())
    private val callMonitor = CallMonitor(application)

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var proofCheckJob: Job? = null
    private var proofChecksPassed = 0
    private var proofChecksTotal = 0
    private var lastProofCheckSecond = 0

    private val quotes = listOf(
        "The secret of getting ahead is getting started.",
        "Focus on being productive instead of busy.",
        "Your future is created by what you do today.",
        "Small progress is still progress.",
        "Don't watch the clock; do what it does. Keep going.",
        "The only way to do great work is to love what you do.",
        "Discipline is the bridge between goals and accomplishment.",
        "Success is the sum of small efforts repeated day in and day out.",
        "You don't have to be extreme, just consistent.",
        "The pain of discipline is nothing compared to the pain of regret.",
        "Focus is the key to unlocking your potential.",
        "Every completed pomodoro is a brick in your palace of success."
    )

    init {
        NotificationHelper.createChannels(application)

        // Load stats and mentor info
        viewModelScope.launch {
            val mentor = mentorRepo.getMentor()
            _uiState.update {
                it.copy(
                    mentor = mentor,
                    mentorSetupComplete = mentor?.isSetupComplete ?: false,
                    motivationQuote = quotes.random()
                )
            }
            repo.getDailyStats().collect { stats ->
                val completed = stats.sumOf { it.completed }
                val streak = calculateStreak(stats)
                val totalSessions = stats.sumOf { it.total }
                val successRate = if (totalSessions > 0) (completed * 100) / totalSessions else 0
                val level = getLevel(completed)
                _uiState.update {
                    it.copy(
                        dailyStats = stats,
                        streakDays = streak,
                        totalCompletedSessions = completed,
                        focusScore = successRate,
                        level = level
                    )
                }
            }
        }
    }

    private fun calculateStreak(stats: List<DailyStats>): Int {
        val sorted = stats.sortedByDescending { it.date }
        var streak = 0
        for (day in sorted) {
            if (day.completed > 0) streak++
            else break
        }
        return streak
    }

    private fun getLevel(completed: Int): String = when {
        completed >= 500 -> "💎 Diamond"
        completed >= 200 -> "🏆 Platinum"
        completed >= 100 -> "🥇 Gold"
        completed >= 50 -> "🥈 Silver"
        completed >= 20 -> "🥉 Bronze"
        completed >= 5 -> "⭐ Rookie"
        else -> "🌱 Beginner"
    }

    fun showTaskDialog() {
        _uiState.update { it.copy(showTaskDialog = true) }
    }

    fun dismissTaskDialog() {
        _uiState.update { it.copy(showTaskDialog = false) }
    }

    fun startSession(taskName: String) {
        if (_uiState.value.timerState == TimerState.RUNNING ||
            _uiState.value.timerState == TimerState.PAUSED) return

        _uiState.update {
            it.copy(
                timerState = TimerState.RUNNING,
                secondsRemaining = 1500,
                currentSessionStart = System.currentTimeMillis(),
                currentTaskName = taskName,
                proofChallenge = null,
                proofChallengeResult = null,
                proofChallengeRemainingAttempts = 3,
                showTaskDialog = false
            )
        }

        proofChecksPassed = 0; proofChecksTotal = 0; lastProofCheckSecond = 0
        startForegroundService()

        // Notify mentor: session started
        viewModelScope.launch {
            val mentor = _uiState.value.mentor
            MentorSyncManager.notifyMentor(getApplication(), mentor, SessionUpdate(
                type = "STARTED",
                taskName = taskName,
                message = "Student started a focus session on: $taskName"
            ))
        }

        // Start call monitoring
        callMonitor.startMonitoring { number, duration ->
            viewModelScope.launch {
                val mentor = _uiState.value.mentor
                MentorSyncManager.notifyMentor(getApplication(), mentor, SessionUpdate(
                    type = "CALL_RECEIVED",
                    taskName = _uiState.value.currentTaskName,
                    callFrom = number,
                    callDuration = duration,
                    message = "Call received from $number during focus session"
                ))
            }
        }

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
            // Notify mentor
            viewModelScope.launch {
                MentorSyncManager.notifyMentor(getApplication(), _uiState.value.mentor, SessionUpdate(
                    type = "CHALLENGE", taskName = _uiState.value.currentTaskName,
                    proofChecksPassed = proofChecksPassed, proofChecksTotal = proofChecksTotal,
                    message = "Proof check passed ($proofChecksPassed/$proofChecksTotal)"
                ))
            }
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
        val task = _uiState.value.currentTaskName
        failSession("Session was manually skipped")
        viewModelScope.launch {
            start?.let { repo.recordSession(it, 1500, "SKIPPED", proofChecksPassed, proofChecksTotal) }
        }
    }

    private fun failSession(reason: String) {
        timerJob?.cancel(); proofCheckJob?.cancel()
        callMonitor.stopMonitoring()
        val start = _uiState.value.currentSessionStart
        val task = _uiState.value.currentTaskName
        _uiState.update { it.copy(timerState = TimerState.FAILED, proofChallenge = null, secondsRemaining = 0) }
        stopForegroundService()
        viewModelScope.launch {
            start?.let { repo.recordSession(it,1500,"FAILED",proofChecksPassed,proofChecksTotal) }
            MentorSyncManager.notifyMentor(getApplication(), _uiState.value.mentor, SessionUpdate(
                type = "FAILED", taskName = task,
                proofChecksPassed = proofChecksPassed, proofChecksTotal = proofChecksTotal,
                message = reason
            ))
        }
        NotificationHelper.triggerRedAlert(getApplication(), reason)
    }

    private fun completeSession() {
        timerJob?.cancel(); proofCheckJob?.cancel()
        callMonitor.stopMonitoring()
        val start = _uiState.value.currentSessionStart
        val task = _uiState.value.currentTaskName
        _uiState.update { it.copy(timerState = TimerState.FINISHED, proofChallenge = null, secondsRemaining = 0) }
        stopForegroundService(); NotificationHelper.cancelTimerNotification(getApplication())
        viewModelScope.launch {
            start?.let { repo.recordSession(it,1500,"COMPLETED",proofChecksPassed,max(proofChecksTotal,1)) }
            MentorSyncManager.notifyMentor(getApplication(), _uiState.value.mentor, SessionUpdate(
                type = "COMPLETED", taskName = task,
                proofChecksPassed = proofChecksPassed, proofChecksTotal = proofChecksTotal,
                message = "Session completed successfully!"
            ))
        }
    }

    fun dismissAlert() { NotificationHelper.cancelRedAlert(getApplication()); _uiState.update { it.copy(timerState = TimerState.IDLE) } }

    fun resetState() {
        timerJob?.cancel(); proofCheckJob?.cancel()
        callMonitor.stopMonitoring()
        stopForegroundService(); NotificationHelper.cancelTimerNotification(getApplication())
        _uiState.update { PomodoroUiState(mentorSetupComplete = _uiState.value.mentorSetupComplete, mentor = _uiState.value.mentor, motivationQuote = quotes.random()) }
    }

    fun saveMentor(name: String, phone: String, email: String, webhook: String) {
        viewModelScope.launch {
            val mentor = MentorDetails(name = name, phone = phone, email = email, webhookUrl = webhook, isSetupComplete = true)
            mentorRepo.saveMentor(mentor)
            _uiState.update { it.copy(mentor = mentor, mentorSetupComplete = true) }
        }
    }

    fun skipMentorSetup() {
        _uiState.update { it.copy(mentorSetupComplete = true) }
    }

    private fun generateProofChallenge(): ProofChallenge {
        val r = Random.Default
        return when (r.nextInt(5)) {
            0 -> { val a=r.nextInt(10,100); val b=r.nextInt(1,50); ProofChallenge("$a + $b = ?", a+b) }
            1 -> { val a=r.nextInt(20,100); val b=r.nextInt(1,20); ProofChallenge("$a - $b = ?", a-b) }
            2 -> { val a=r.nextInt(2,15); val b=r.nextInt(2,12); ProofChallenge("$a × $b = ?", a*b) }
            3 -> { val a=r.nextInt(5,20); val b=r.nextInt(1,10); ProofChallenge("$a ÷ $b = ? (round down)", a/b) }
            4 -> { val n=List(r.nextInt(1,8)){r.nextInt(1,50)}; ProofChallenge("Sum of ${n.joinToString(", ")} = ?", n.sum()) }
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
    override fun onCleared() { timerJob?.cancel(); proofCheckJob?.cancel(); callMonitor.stopMonitoring(); super.onCleared() }
}
