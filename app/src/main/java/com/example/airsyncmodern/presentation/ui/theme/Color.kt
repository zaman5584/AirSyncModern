package com.example.airsyncmodern.presentation.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Material 3 Dark theme color palette for Air Sync Remote Z
 * Based on DESIGN.md specification with OLED-friendly deep colors
 */

// Background & Surface Colors
val md_theme_dark_background = Color(0xFF0B0E14)           // Deep OLED Black/Navy
val md_theme_dark_surface = Color(0xFF141822)              // Card backgrounds, remote chassis
val md_theme_dark_surfaceVariant = Color(0xFF1E2433)       // Inactive buttons, D-Pad base ring
val md_theme_dark_surfaceContainerHigh = Color(0xFF273042) // Pressed states, elevated toolbars

// Primary & Accent Colors
val md_theme_dark_primary = Color(0xFF00D2FF)              // Active accent, selected mode, glow
val md_theme_dark_onPrimary = Color(0xFF003544)            // High contrast icon on primary
val md_theme_dark_secondary = Color(0xFFFF9E00)            // Alert/warning, power button accent

// Text & Icon Colors
val md_theme_dark_onSurface = Color(0xFFF0F3FA)            // Primary text and active icons
val md_theme_dark_onSurfaceVariant = Color(0xFF8C97AC)     // Secondary labels, inactive icons

// Status Colors
val md_theme_dark_success = Color(0xFF00E676)              // Connected status dot
val md_theme_dark_error = Color(0xFFFF5252)                // Connection lost / timeout

// Additional UI Colors
val md_theme_dark_outline = Color(0xFF3E4758)              // Dividers and borders
val md_theme_dark_outlineVariant = Color(0xFF2A2F3D)       // Subtle dividers

// Button State Colors
val buttonPressedOverlay = Color(0x20FFFFFF)               // White overlay at 12% opacity
val rippleColor = Color(0x30FFFFFF)                        // Ripple effect color

// Gradient Colors for OK Button
val primaryGradientStart = Color(0xFF00D2FF)
val primaryGradientEnd = Color(0xFF0088CC)
