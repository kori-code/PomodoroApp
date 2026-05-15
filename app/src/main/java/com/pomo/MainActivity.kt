package com.pomo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.pomo.auth.AuthScreen
import com.pomo.auth.UserRole
import com.pomo.dashboard.MentorDashboard
import com.pomo.dashboard.StudentDashboard
import com.pomo.ui.KoriTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            KoriTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var isAuthenticated by remember { mutableStateOf(false) }
    var currentUserRole by remember { mutableStateOf<UserRole?>(null) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    var showRoleSelection by remember { mutableStateOf(true) }
    
    if (!isAuthenticated) {
        AuthScreen(
            onAuthSuccess = { user ->
                isAuthenticated = true
                currentUserRole = user.role
                currentUserId = user.id
                showRoleSelection = false
            },
            onBackToRoleSelection = {
                showRoleSelection = true
            }
        )
    } else {
        when (currentUserRole) {
            UserRole.STUDENT -> {
                StudentDashboard(
                    userId = currentUserId ?: return,
                    onLogout = {
                        isAuthenticated = false
                        currentUserRole = null
                        currentUserId = null
                        showRoleSelection = true
                    }
                )
            }
            UserRole.MENTOR -> {
                MentorDashboard(
                    mentorId = currentUserId ?: return,
                    onLogout = {
                        isAuthenticated = false
                        currentUserRole = null
                        currentUserId = null
                        showRoleSelection = true
                    }
                )
            }
            null -> {}
        }
    }
}
