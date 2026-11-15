package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudioColorScheme = darkColorScheme(
    primary = MotionCyan,
    onPrimary = Color(0xFF002026),
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFF70F3FF),
    secondary = KeyframeViolet,
    onSecondary = Color(0xFF25004D),
    secondaryContainer = Color(0xFF4D1A7E),
    onSecondaryContainer = Color(0xFFE9B8FF),
    tertiary = PlayheadAmber,
    onTertiary = Color(0xFF3B2500),
    error = CutRose,
    onError = Color.White,
    background = StudioDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = StudioSurface,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = StudioBorder,
    outlineVariant = StudioDivider
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioColorScheme,
        typography = Typography,
        content = content
    )
}
