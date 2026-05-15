package com.pomo.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: (User) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    var showRoleSelection by remember { mutableStateOf(true) }
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated && uiState.currentUser != null) {
            onAuthSuccess(uiState.currentUser!!)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🍅", fontSize = 48.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Kori Pomodoro",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                "Focus with Accountability",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            AnimatedVisibility(
                visible = showRoleSelection,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "I am a...",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    RoleCard(
                        title = "Student",
                        description = "Focus on your tasks with accountability",
                        icon = Icons.Default.School,
                        onClick = {
                            selectedRole = UserRole.STUDENT
                            showRoleSelection = false
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    RoleCard(
                        title = "Mentor / Parent",
                        description = "Monitor and guide your students",
                        icon = Icons.Default.People,
                        onClick = {
                            selectedRole = UserRole.MENTOR
                            showRoleSelection = false
                        }
                    )
                }
            }
            
            AnimatedVisibility(
                visible = !showRoleSelection && selectedRole != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            if (isLoginMode) "Welcome Back!" else "Create Account",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            if (isLoginMode) "Sign in to continue" else "Get started with Kori",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (!isLoginMode) {
                            OutlinedTextField(
                                value = uiState.fullName,
                                onValueChange = { viewModel.updateFullName(it) },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = uiState.dateOfBirth,
                                onValueChange = { viewModel.updateDateOfBirth(it) },
                                label = { Text("Date of Birth (YYYY-MM-DD)") },
                                leadingIcon = { Icon(Icons.Default.Cake, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        
                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = { viewModel.updateEmail(it) },
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = uiState.phoneNumber,
                            onValueChange = { viewModel.updatePhoneNumber(it) },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (!isLoginMode && selectedRole == UserRole.STUDENT) {
                            OutlinedTextField(
                                value = uiState.mentorId,
                                onValueChange = { viewModel.updateMentorId(it) },
                                label = { Text("Mentor ID (provided by your mentor)") },
                                leadingIcon = { Icon(Icons.Default.Badge, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("Enter 6-digit mentor ID") }
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = uiState.mentorPhone,
                                onValueChange = { viewModel.updateMentorPhone(it) },
                                label = { Text("Mentor's Phone Number (verification)") },
                                leadingIcon = { Icon(Icons.Default.Verified, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                if (isLoginMode) {
                                    viewModel.login(
                                        email = uiState.email,
                                        phoneNumber = uiState.phoneNumber,
                                        role = selectedRole!!
                                    )
                                } else {
                                    viewModel.signUp(
                                        role = selectedRole!!,
                                        fullName = uiState.fullName,
                                        dateOfBirth = uiState.dateOfBirth,
                                        phoneNumber = uiState.phoneNumber,
                                        email = uiState.email,
                                        mentorId = uiState.mentorId,
                                        mentorPhone = uiState.mentorPhone
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(if (isLoginMode) "Sign In" else "Create Account", fontSize = 16.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        TextButton(
                            onClick = { isLoginMode = !isLoginMode },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                if (isLoginMode) "Don't have an account? Sign Up" else "Already have an account? Sign In",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        if (uiState.errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    uiState.errorMessage!!,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
