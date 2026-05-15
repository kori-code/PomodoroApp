// app/src/main/java/com/pomo/pomodoro/ProofChallenge.kt
package com.pomo.pomodoro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ProofChallengeDialog(
    challenge: PomodoroEngine.ProofChallenge,
    onCorrect: () -> Unit,
    onWrong: () -> Unit
) {
    var answer by remember { mutableStateOf("") }
    var attemptsLeft by remember { mutableStateOf(challenge.attemptsLeft) }
    var showError by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🧠 Proof of Focus",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Solve this to continue your session",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    challenge.question,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = answer,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() } && it.length <= 5) {
                            answer = it
                            showError = false
                        }
                    },
                    label = { Text("Your answer") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError,
                    supportingText = {
                        if (showError) {
                            Text(
                                "Incorrect answer! ${attemptsLeft - 1} attempts left",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Attempts remaining: $attemptsLeft",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        val userAnswer = answer.toIntOrNull()
                        if (userAnswer != null && userAnswer == challenge.answer) {
                            onCorrect()
                        } else {
                            attemptsLeft--
                            showError = true
                            answer = ""
                            if (attemptsLeft <= 0) {
                                onWrong()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = answer.isNotEmpty()
                ) {
                    Text("Submit Answer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
