package com.altsendme.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * AltSendme color palette - matching the desktop app design
 */
object AppColors {
    // Background colors
    val Background = Color(0xFF191919)
    val MainView = Color(0xFF191919)
    val Surface = Color(0xFF191919)

    // Primary colors
    val Primary = Color(0xAF25D365)      // Green with opacity (matching desktop)
    val PrimaryBright = Color(0xFF25D365) // Full green
    val AccentLight = Color(0xFF2D78DC)   // Blue
    val Accent = Color(0xFF363636)        // Gray
    val Destructive = Color(0xFF903C3C)   // Red

    // Text colors
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xB3FFFFFF) // 70% white
    val TextMuted = Color(0x99FFFFFF)     // 60% white
    val TextHint = Color(0x66FFFFFF)      // 40% white

    // Glass effect colors
    val GlassBorder = Color(0x1AFFFFFF)       // 10% white
    val GlassBorderLight = Color(0x0DFFFFFF)  // 5% white
    val GlassBorderStrong = Color(0x33FFFFFF) // 20% white

    // Tab colors
    val TabBackground = Color(0x1AFFFFFF)     // 10% white
    val TabSelected = Color(0xFF191919)
    val TabSelectedBorder = Color(0x33FFFFFF) // 20% white

    // Progress colors
    val ProgressBackground = Color(0x1AFFFFFF)
    val ProgressFill = Color(0xAF25D365)

    // Status colors
    val StatusListening = Color(0xFF808080)  // Gray
    val StatusActive = Color(0xFF25D365)     // Green
    val StatusCompleted = Color(0xFF2D78DC)  // Blue
    val StatusError = Color(0xFFEF4444)      // Red

    // Button colors
    val ButtonPrimary = Primary
    val ButtonSecondary = Accent
    val ButtonDestructive = Destructive
    val ButtonDisabled = Color(0x4D363636)

    // Input colors
    val InputBackground = Color(0x1AFFFFFF)
    val InputBorder = Color(0x1AFFFFFF)
    val InputFocusedBorder = Color(0x33FFFFFF)
}
