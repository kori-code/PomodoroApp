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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pomo.data.DailyStats
import com.pomo.notification.NotificationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create notification channels early
        NotificationHelper.createChannels(this)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFEF5350),       // Pomodoro red
                    secondary = Color(0xFF66BB6A),      // Success green
                    tertiary = Color(0xFFFFCA28),       // Warning amber
                    background = Color(0xFF1A1A2E),
                    surface = Color(0xFF16213E),
                    onPrimary = Color.White,
                    onSecondary = Color.White,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                PomodoroApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroApp(viewModel: PomodoroViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "PomoProof",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
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
            // Main Timer Display
            TimerDisplay(state)

            Spacer(modifier = Modifier.height(16.dp))

            // State indicator
            TimerStateIndicator(state)

            Spacer(modifier = Modifier.height(16.dp))

            // --- PROOF OF ACTIVITY CHALLENGE ---
            if (state.proofChallenge != null) {
                ProofChallengeCard(
                    challenge = state.proofChallenge!!,
                    remainingAttempts = state.proofChallengeRemainingAttempts,
                    lastResult = state.proofChallengeResult,
                    onSubmit = { answer -> viewModel.submitProof(answer) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Control Buttons
            ControlButtons(
                state = state,
                onStart = { viewModel.startSession() },
                onSkip = { viewModel.skipSession() },
                onDismiss = { viewModel.dismissAlert() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Daily Statistics
            Text(
                "Daily Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            DailyStatsTable(stats = state.dailyStats)
        }
    }
}

@Composable
fun TimerDisplay(state: PomodoroUiState) {
    val minutes = state.secondsRemaining / 60
    val seconds = state.secondsRemaining % 60
    val timeStr = "${minutes}:${String.format("%02d", seconds)}"

    val timerColor = when (state.timerState) {
        TimerState.RUNNING -> MaterialTheme.colorScheme.primary
        TimerState.PAUSED -> MaterialTheme.colorScheme.tertiary
        TimerState.FAILED, TimerState.SKIPPED -> Color(0xFFD32F2F)
        TimerState.FINISHED -> MaterialTheme.colorScheme.secondary
        TimerState.IDLE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    Box(
        modifier = Modifier
            .size(280.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeStr,
                fontSize = 64.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = timerColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = when (state.timerState) {
                    TimerState.IDLE -> "Ready to focus"
                    TimerState.RUNNING -> "Focus!"
                    TimerState.PAUSED -> "Proof required"
                    TimerState.FAILED -> "SESSION FAILED"
                    TimerState.SKIPPED -> "SKIPPED"
                    TimerState.FINISHED -> "COMPLETED!"
                },
                fontSize = 16.sp,
                color = timerColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun TimerStateIndicator(state: PomodoroUiState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (text, color) = when (state.timerState) {
            TimerState.IDLE -> "Press Start to begin" to MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            TimerState.RUNNING -> "Working..." to MaterialTheme.colorScheme.primary
            TimerState.PAUSED -> "Answer to continue" to MaterialTheme.colorScheme.tertiary
            TimerState.FAILED -> "FAILED — Red Alert!" to Color(0xFFD32F2F)
            TimerState.SKIPPED -> "Skipped" to Color(0xFFFF6F00)
            TimerState.FINISHED -> "Well done!" to MaterialTheme.colorScheme.secondary
        }
        Text(text, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ProofChallengeCard(
    challenge: com.pomo.ProofChallenge,
    remainingAttempts: Int,
    lastResult: String?,
    onSubmit: (Int) -> Unit
) {
    var userAnswer by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "⚡ Proof of Focus Required!",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                challenge.question,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = userAnswer,
                    onValueChange = { newVal ->
                        if (newVal.all { it.isDigit() } && newVal.length <= 5) {
                            userAnswer = newVal
                        }
                    },
                    label = { Text("Answer") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        val answer = userAnswer.toIntOrNull()
                        if (answer != null) {
                            onSubmit(answer)
                            userAnswer = ""
                        }
                    },
                    enabled = userAnswer.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Submit")
                }
            }

            if (lastResult != null) {
                val resultColor = if (lastResult == "correct") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                Text(
                    if (lastResult == "correct") "✓ Correct!" else "✗ Wrong!",
                    color = resultColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "Attempts remaining: $remainingAttempts",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ControlButtons(
    state: PomodoroUiState,
    onStart: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state.timerState) {
            TimerState.IDLE -> {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .height(56.dp)
                        .widthIn(min = 180.dp)
                ) {
                    Text("Start Session", fontSize = 18.sp)
                }
            }
            TimerState.RUNNING, TimerState.PAUSED -> {
                Button(
                    onClick = onSkip,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    ),
                    modifier = Modifier
                        .height(56.dp)
                        .widthIn(min = 180.dp)
                ) {
                    Text("✗ Skip Session", fontSize = 18.sp)
                }
            }
            TimerState.FAILED, TimerState.SKIPPED -> {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ),
                    modifier = Modifier
                        .height(56.dp)
                        .widthIn(min = 180.dp)
                ) {
                    Text("✓ Dismiss Alert", fontSize = 18.sp)
                }
            }
            TimerState.FINISHED -> {
                Button(
                    onClick = { /* handled by reset */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier
                        .height(56.dp)
                        .widthIn(min = 180.dp)
                ) {
                    Text("✓ Session Complete!", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun DailyStatsTable(stats: List<DailyStats>) {
    if (stats.isEmpty()) {
        Text(
            "No sessions yet. Start your first Pomodoro!",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 16.dp)
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Date", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("✅", fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                Text("❌", fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                Text("⏭️", fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
            }
        }

        items(stats.take(10)) { day ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    day.date,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${day.completed}",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp)
                )
                Text(
                    "${day.failed}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp)
                )
                Text(
                    "${day.skipped}",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp)
                )
            }
        }
    }
}
