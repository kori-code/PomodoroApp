package com.pomo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pomo.auth.AuthScreen
import com.pomo.auth.AuthViewModel
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
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()
    
    // Check if user is already logged in
    LaunchedEffect(Unit) {
        // AuthViewModel already checks for logged-in user in init
    }
    
    if (!authState.isAuthenticated || authState.currentUser == null) {
        AuthScreen(
            onAuthSuccess = { user ->
                // Auth handled by ViewModel
            },
            onBackToRoleSelection = { },
            viewModel = authViewModel
        )
    } else {
        val currentUser = authState.currentUser!!
        
        when (currentUser.role) {
            UserRole.STUDENT -> {
                StudentDashboard(
                    userId = currentUser.id,
                    onLogout = {
                        authViewModel.logout()
                    }
                )
            }
            UserRole.MENTOR -> {
                MentorDashboard(
                    mentorId = currentUser.id,
                    onLogout = {
                        authViewModel.logout()
                    }
                )
            }
        }
    }
}
