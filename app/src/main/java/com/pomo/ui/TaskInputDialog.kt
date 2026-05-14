package com.pomo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TaskPreset(
    val icon: @Composable () -> Unit,
    val name: String
)

@Composable
fun TaskInputDialog(
    onStart: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customTask by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<String?>(null) }

    val presets = listOf(
        TaskPreset({ Text("📚") }, "Study - Reading"),
        TaskPreset({ Text("✏️") }, "Study - Writing"),
        TaskPreset({ Text("🧮") }, "Study - Math"),
        TaskPreset({ Text("💻") }, "Coding / Programming"),
        TaskPreset({ Text("📝") }, "Assignment / Homework"),
        TaskPreset({ Text("🎓") }, "Exam Preparation"),
        TaskPreset({ Text("📊") }, "Office Work"),
        TaskPreset({ Text("🎨") }, "Creative Work"),
        TaskPreset({ Text("🧪") }, "Lab / Research"),
        TaskPreset({ Text("📖") }, "Other - Custom")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "🎯 What are you working on?",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Choose a task or type your own:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(Modifier.height(12.dp))

                // Preset chips
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = selectedPreset == preset.name,
                            onClick = {
                                selectedPreset = preset.name
                                customTask = ""
                            },
                            label = { Text(preset.name, fontSize = 12.sp) },
                            leadingIcon = {
                                Text(preset.name.take(2))
                            }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val task = selectedPreset ?: customTask
                    if (task.isNotBlank()) {
                        onStart(task)
                    }
                },
                enabled = (selectedPreset != null || customTask.isNotBlank())
            ) {
                Text("🚀 Start Focus Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
