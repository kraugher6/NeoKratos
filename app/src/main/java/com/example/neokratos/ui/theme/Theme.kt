package com.example.neokratos.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * BRUTAL DARK THEME
 *
 * Deep blacks, strong contrast, high-energy orange.
 * Made for gyms with harsh lighting.
 * Every number jumps out at you.
 */
private val BrutalDarkColorScheme = darkColorScheme(
    // Primary - ENERGY orange/red for action buttons
    primary = Color(0xFFFF6B35),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2A1410),
    onPrimaryContainer = Color(0xFFFFB59D),

    // Secondary - muted grays
    secondary = Color(0xFF4A4A4A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = Color(0xFFCCCCCC),

    // Tertiary
    tertiary = Color(0xFF757575),
    onTertiary = Color.White,

    // Background - DEEP black
    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFE8E8E8),

    // Surface - slightly lighter black
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFB8B8B8),

    // Error - bright red for warnings
    error = Color(0xFFFF453A),
    onError = Color.Black,
    errorContainer = Color(0xFF3A0F0D),
    onErrorContainer = Color(0xFFFFBDB5),

    // Outline
    outline = Color(0xFF3D3D3D),
    outlineVariant = Color(0xFF2A2A2A)
)

@Composable
fun NeoKratosTheme(
    darkTheme: Boolean = true, // ALWAYS dark - we're not savages
    dynamicColor: Boolean = false, // NO dynamic color - we control everything
    content: @Composable () -> Unit
) {
    val colorScheme = BrutalDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BrutalTypography,
        content = content
    )
}