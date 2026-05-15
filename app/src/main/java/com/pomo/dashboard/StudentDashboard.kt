// app/src/main/java/com/pomo/dashboard/StudentDashboard.kt
package com.pomo.dashboard

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pomo.pomodoro.PomodoroEngine
import com.pomo.pomodoro.ProofChallengeDialog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    userId: String,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTaskDialog by remember { mutableStateOf(false) }
    var showProofDialog by remember { mutableStateOf(false) }
    var currentChallenge by remember { mutableStateOf<PomodoroEngine.ProofChallenge?>(null) }
    var drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(userId) {
        viewModel.loadUserData(userId)
    }
    
    // Listen for proof challenges
    LaunchedEffect(Unit) {
        PomodoroEngine.onProofChallenge.collect { challenge ->
            currentChallenge = challenge
            showProofDialog = true
            viewModel.pauseTimer()
        }
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(end = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // User Profile Header
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                uiState.userFullName.take(2).uppercase(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        uiState.userFullName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        uiState.userEmail,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    // Menu Items
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Edit, null) },
                        label = { Text("Edit Profile") },
                        selected = false,
                        onClick = { /* TODO: Edit profile */ }
                    )
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.History, null) },
                        label = { Text("Session History") },
                        selected = false,
                        onClick = { /* TODO: Show history */ }
                    )
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Settings") },
                        selected = false,
                        onClick = { /* TODO: Settings */ }
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Logout, null) },
                        label = { Text("Logout") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.logout()
                            onLogout()
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Kori Pomodoro",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (uiState.currentTask.isNotBlank() && uiState.timerState != PomodoroEngine.TimerState.IDLE) {
                                Text(
                                    uiState.currentTask,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        if (uiState.streakDays > 0) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFFF6B35).copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔥", fontSize = 14.sp)
                                    Text(
                                        " ${uiState.streakDays}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF6B35)
                                    )
                                }
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCard(
                            label = "Focus Score",
                            value = "${uiState.focusScore}%",
                            icon = Icons.Default.TrendingUp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        StatCard(
                            label = "Completed",
                            value = "${uiState.totalCompleted}",
                            icon = Icons.Default.CheckCircle,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        StatCard(
                            label = "Level",
                            value = uiState.level,
                            icon = Icons.Default.EmojiEvents,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Timer Display
                    TimerDisplay(
                        secondsRemaining = uiState.secondsRemaining,
                        timerState = uiState.timerState
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Current Task
                    if (uiState.currentTask.isNotBlank() && uiState.timerState != PomodoroEngine.TimerState.IDLE) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📌", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    uiState.currentTask,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Control Buttons
                    when (uiState.timerState) {
                        PomodoroEngine.TimerState.IDLE -> {
                            Button(
                                onClick = { showTaskDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Focus Session", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        PomodoroEngine.TimerState.RUNNING -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.pauseTimer() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Pause, null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pause")
                                }
                                
                                Button(
                                    onClick = { viewModel.failSession("Manual skip") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.Default.Close, null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Skip")
                                }
                            }
                        }
                        
                        PomodoroEngine.TimerState.PAUSED -> {
                            Button(
                                onClick = { viewModel.resumeTimer() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Resume Session", fontSize = 18.sp)
                            }
                        }
                        
                        PomodoroEngine.TimerState.COMPLETED -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🎉", fontSize = 48.sp)
                                    Text(
                                        "Session Completed!",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        "Great job staying focused!",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.resetTimer() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Start New Session")
                                    }
                                }
                            }
                        }
                        
                        PomodoroEngine.TimerState.FAILED -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("⚠️", fontSize = 48.sp)
                                    Text(
                                        "Session Failed",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        "Stay focused next time!",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.resetTimer() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Try Again")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Task Input Dialog
    if (showTaskDialog) {
        TaskInputDialog(
            onStart = { task ->
                showTaskDialog = false
                viewModel.startSession(task)
            },
            onDismiss = { showTaskDialog = false }
        )
    }
    
    // Proof Challenge Dialog
    if (showProofDialog && currentChallenge != null) {
        ProofChallengeDialog(
            challenge = currentChallenge!!,
            onCorrect = {
                showProofDialog = false
                viewModel.resumeTimer()
            },
            onWrong = {
                // Keep dialog open, allow retry
                viewModel.proofCheckFailed()
                if (currentChallenge!!.attemptsLeft <= 1) {
                    showProofDialog = false
                    viewModel.failSession("Proof challenge failed")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TimerDisplay(secondsRemaining: Int, timerState: PomodoroEngine.TimerState) {
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)
    
    val gradientBrush = when (timerState) {
        PomodoroEngine.TimerState.RUNNING -> Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary
            )
        )
        PomodoroEngine.TimerState.PAUSED -> Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary
            )
        )
        else -> Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        )
    }
    
    Card(
        modifier = Modifier
            .size(280.dp)
            .clip(CircleShape),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush, alpha = 0.1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    timeString,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    when (timerState) {
                        PomodoroEngine.TimerState.IDLE -> "Ready to focus"
                        PomodoroEngine.TimerState.RUNNING -> "Focus Mode 🔒"
                        PomodoroEngine.TimerState.PAUSED -> "Verification Required 🧠"
                        PomodoroEngine.TimerState.COMPLETED -> "Complete! 🎉"
                        PomodoroEngine.TimerState.FAILED -> "Failed ❌"
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
