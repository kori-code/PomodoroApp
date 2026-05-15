// app/src/main/java/com/pomo/ui/Theme.kt
package com.pomo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Professional Deep Indigo & Amber palette
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),      // Vibrant Indigo
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1E1B4B),
    onPrimaryContainer = Color(0xFFC7D2FE),
    secondary = Color(0xFFF59E0B),    // Amber/Gold
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF451A03),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = Color(0xFF10B981),     // Emerald Green
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFF0F172A),   // Slate 900
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),      // Slate 800
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF475569)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6366F1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFFF59E0B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = Color(0xFF10B981),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun KoriTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    
    SideEffect {
        val window = (view.context as androidx.activity.ComponentActivity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            displayLarge = MaterialTheme.typography.displayLarge.copy(color = colorScheme.onBackground),
            displayMedium = MaterialTheme.typography.displayMedium.copy(color = colorScheme.onBackground),
            displaySmall = MaterialTheme.typography.displaySmall.copy(color = colorScheme.onBackground),
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(color = colorScheme.onBackground),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(color = colorScheme.onBackground),
            headlineSmall = MaterialTheme.typography.headlineSmall.copy(color = colorScheme.onBackground),
            titleLarge = MaterialTheme.typography.titleLarge.copy(color = colorScheme.onBackground),
            titleMedium = MaterialTheme.typography.titleMedium.copy(color = colorScheme.onBackground),
            titleSmall = MaterialTheme.typography.titleSmall.copy(color = colorScheme.onBackground),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(color = colorScheme.onBackground),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onBackground),
            bodySmall = MaterialTheme.typography.bodySmall.copy(color = colorScheme.onBackground)
        ),
        content = content
    )
}
