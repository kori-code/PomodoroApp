// app/src/main/java/com/pomo/pomodoro/TaskInputDialog.kt
package com.pomo.pomodoro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

data class TaskPreset(val emoji: String, val name: String)

@Composable
fun TaskInputDialog(
    onStart: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customTask by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<String?>(null) }
    
    val presets = listOf(
        TaskPreset("📚", "Study - Reading"),
        TaskPreset("✏️", "Study - Writing"),
        TaskPreset("🧮", "Study - Math"),
        TaskPreset("💻", "Coding"),
        TaskPreset("📝", "Homework"),
        TaskPreset("🎓", "Exam Prep"),
        TaskPreset("📊", "Office Work"),
        TaskPreset("🎨", "Creative Work")
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
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
                    "🎯 What are you working on?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Choose a task or type your own",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(presets) { preset ->
                        FilterChip(
                            selected = selectedPreset == preset.name,
                            onClick = {
                                selectedPreset = preset.name
                                customTask = ""
                            },
                            label = { Text("${preset.emoji} ${preset.name}", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = customTask,
                    onValueChange = {
                        customTask = it
                        selectedPreset = null
                    },
                    label = { Text("Or type custom task...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val task = selectedPreset ?: customTask
                            if (task.isNotBlank()) {
                                onStart(task)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedPreset != null || customTask.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Start Session")
                    }
                }
            }
        }
    }
}
