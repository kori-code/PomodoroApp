package com.pomo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pomo.data.DailyStats
import com.pomo.notification.NotificationHelper
import com.pomo.ui.MentorSetupScreen
import com.pomo.ui.TaskInputDialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannels(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFFEF5350), secondary = Color(0xFF66BB6A),
                tertiary = Color(0xFFFFCA28), background = Color(0xFF1A1A2E),
                surface = Color(0xFF16213E), onPrimary = Color.White,
                onSecondary = Color.White, onBackground = Color.White, onSurface = Color.White
            )) { KoriPomodoroApp() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KoriPomodoroApp(viewModel: PomodoroViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Show mentor setup if not completed
    if (!state.mentorSetupComplete) {
        MentorSetupScreen(
            currentName = state.mentor?.name ?: "",
            currentPhone = state.mentor?.phone ?: "",
            currentEmail = state.mentor?.email ?: "",
            currentWebhook = state.mentor?.webhookUrl ?: "",
            onSave = { name, phone, email, webhook ->
                viewModel.saveMentor(name, phone, email, webhook)
            },
            onSkip = { viewModel.skipMentorSetup() }
        )
        return
    }

    // Show task dialog when starting
    if (state.showTaskDialog) {
        TaskInputDialog(
            onStart = { task -> viewModel.startSession(task) },
            onDismiss = { viewModel.dismissTaskDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🍅 Kori Pomodoro", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (state.level.isNotBlank()) {
                            Text(state.level, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Streak indicator
                    if (state.streakDays > 0) {
                        Text(
                            "🔥 ${state.streakDays}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B35)
                        )
                    }
                    if (state.timerState != TimerState.IDLE) {
                        TextButton(onClick = { viewModel.resetState() }) {
                            Text("Reset", color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Motivation quote
            if (state.timerState == TimerState.IDLE && state.motivationQuote.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "\"${state.motivationQuote}\"",
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // Timer display
            TimerDisplay(state)
            Spacer(Modifier.height(12.dp))

            // Current task name
            if (state.currentTaskName.isNotBlank() && state.timerState != TimerState.IDLE) {
                Text(
                    "📌 ${state.currentTaskName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(Modifier.height(8.dp))
            }

            // State indicator
            TimerStateIndicator(state)
            Spacer(Modifier.height(12.dp))

            // Focus score gauge
            if (state.timerState == TimerState.IDLE && state.focusScore > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("🎯 Focus", "${state.focusScore}%")
                        StatItem("✅ Done", "${state.totalCompletedSessions}")
                        StatItem("🔥 Streak", "${state.streakDays}d")
                        StatItem("🏆 Level", state.level.split(" ").last())
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Proof challenge
            if (state.proofChallenge != null) {
                ProofChallengeCard(
                    state.proofChallenge!!, state.proofChallengeRemainingAttempts,
                    state.proofChallengeResult) { viewModel.submitProof(it) }
                Spacer(Modifier.height(12.dp))
            }

            // Controls
            ControlButtons(state,
                onStart = { viewModel.showTaskDialog() },
                onSkip = { viewModel.skipSession() },
                onDismiss = { viewModel.dismissAlert() }
            )

            Spacer(Modifier.height(16.dp))

            // Daily stats table
            if (state.dailyStats.isNotEmpty()) {
                Text("📊 Daily History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(8.dp))
                DailyStatsTable(state.dailyStats)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
fun TimerDisplay(state: PomodoroUiState) {
    val m = state.secondsRemaining / 60; val s = state.secondsRemaining % 60
    val c = when (state.timerState) {
        TimerState.RUNNING -> MaterialTheme.colorScheme.primary
        TimerState.PAUSED -> MaterialTheme.colorScheme.tertiary
        TimerState.FAILED, TimerState.SKIPPED -> Color(0xFFD32F2F)
        TimerState.FINISHED -> MaterialTheme.colorScheme.secondary
        TimerState.IDLE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }
    Box(Modifier.size(260.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$m:${String.format("%02d", s)}", fontSize = 60.sp, fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold, color = c, textAlign = TextAlign.Center)
            Text(when (state.timerState) {
                TimerState.IDLE -> "🍅 Ready"
                TimerState.RUNNING -> "🎯 Focus!"
                TimerState.PAUSED -> "🧠 Proof required"
                TimerState.FAILED -> "❌ FAILED"
                TimerState.SKIPPED -> "⏭️ SKIPPED"
                TimerState.FINISHED -> "🎉 COMPLETED!"
            }, fontSize = 16.sp, color = c.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun TimerStateIndicator(state: PomodoroUiState) {
    val (t, c) = when (state.timerState) {
        TimerState.IDLE -> "Tap Start to begin your focus session" to MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        TimerState.RUNNING -> "Stay focused! You've got this 💪" to MaterialTheme.colorScheme.primary
        TimerState.PAUSED -> "Answer the challenge to continue" to MaterialTheme.colorScheme.tertiary
        TimerState.FAILED -> "Session failed — Red Alert sent!" to Color(0xFFD32F2F)
        TimerState.SKIPPED -> "Session skipped" to Color(0xFFFF6F00)
        TimerState.FINISHED -> "Excellent! Take a 5-min break 🎉" to MaterialTheme.colorScheme.secondary
    }
    Text(t, color = c, fontWeight = FontWeight.Medium, fontSize = 14.sp)
}

@Composable
fun ProofChallengeCard(challenge: ProofChallenge, remaining: Int, lastResult: String?, onSubmit: (Int)->Unit) {
    var answer by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🧠 Proof of Focus!", fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(challenge.question, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(answer, { if (it.all { c->c.isDigit() } && it.length <= 5) answer = it },
                    label = { Text("Answer") }, modifier = Modifier.weight(1f), singleLine = true)
                Button(onClick = { answer.toIntOrNull()?.let { onSubmit(it); answer = "" } },
                    enabled = answer.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Submit") }
            }
            if (lastResult != null) {
                val rc = if (lastResult == "correct") MaterialTheme.colorScheme.secondary
                         else MaterialTheme.colorScheme.primary
                Text(if (lastResult == "correct") "✓ Correct!" else "✗ Wrong!",
                    color = rc, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Text("Attempts left: $remaining", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun ControlButtons(state: PomodoroUiState, onStart:()->Unit, onSkip:()->Unit, onDismiss:()->Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        when (state.timerState) {
            TimerState.IDLE -> Button(onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(56.dp).widthIn(min = 200.dp)) {
                Text("🚀 Start Session", fontSize = 18.sp) }
            TimerState.RUNNING, TimerState.PAUSED -> Button(onClick = onSkip,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                modifier = Modifier.height(56.dp).widthIn(min = 200.dp)) {
                Text("✗ Skip Session", fontSize = 18.sp) }
            TimerState.FAILED, TimerState.SKIPPED -> Button(onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.height(56.dp).widthIn(min = 200.dp)) {
                Text("✓ Dismiss Alert", fontSize = 18.sp) }
            TimerState.FINISHED -> Button(
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.height(56.dp).widthIn(min = 200.dp), onClick = {}
            ) { Text("🎉 Break Time!", fontSize = 18.sp) }
        }
    }
}

@Composable
fun DailyStatsTable(stats: List<DailyStats>) {
    if (stats.isEmpty()) {
        Text("No sessions yet. Start your first Pomodoro!",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 16.dp)); return
    }
    LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Date", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("✅", fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                Text("❌", fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                Text("⏭️", fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
            }
        }
        items(stats.take(10)) { day ->
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(day.date, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${day.completed}", color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                Text("${day.failed}", color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                Text("${day.skipped}", color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
            }
        }
    }
}
