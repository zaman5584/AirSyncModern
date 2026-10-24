package com.example.airsyncmodern.presentation.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Material 3 Dark Color Scheme for Air Sync Remote Z
 * Expressive Dark theme optimized for OLED displays
 */
private val AirSyncDarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    secondary = md_theme_dark_secondary,
    onSecondary = Color.Black,
    error = md_theme_dark_error,
    onError = Color.White,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onSurface,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    surfaceContainerHigh = md_theme_dark_surfaceContainerHigh
)

@Composable
fun AirSyncModernTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AirSyncDarkColorScheme,
        typography = Typography,
        content = content
    )
}
