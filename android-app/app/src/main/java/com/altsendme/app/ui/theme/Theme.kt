package com.altsendme.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Custom color scheme for AltSendme
 */
data class AltSendmeColors(
    val background: Color = AppColors.Background,
    val surface: Color = AppColors.Surface,
    val primary: Color = AppColors.Primary,
    val primaryBright: Color = AppColors.PrimaryBright,
    val accentLight: Color = AppColors.AccentLight,
    val accent: Color = AppColors.Accent,
    val destructive: Color = AppColors.Destructive,
    val textPrimary: Color = AppColors.TextPrimary,
    val textSecondary: Color = AppColors.TextSecondary,
    val textMuted: Color = AppColors.TextMuted,
    val textHint: Color = AppColors.TextHint,
    val glassBorder: Color = AppColors.GlassBorder,
    val glassBorderStrong: Color = AppColors.GlassBorderStrong,
    val tabBackground: Color = AppColors.TabBackground,
    val tabSelected: Color = AppColors.TabSelected,
    val tabSelectedBorder: Color = AppColors.TabSelectedBorder,
    val progressBackground: Color = AppColors.ProgressBackground,
    val progressFill: Color = AppColors.ProgressFill,
    val statusListening: Color = AppColors.StatusListening,
    val statusActive: Color = AppColors.StatusActive,
    val statusCompleted: Color = AppColors.StatusCompleted,
    val statusError: Color = AppColors.StatusError,
    val buttonPrimary: Color = AppColors.ButtonPrimary,
    val buttonSecondary: Color = AppColors.ButtonSecondary,
    val buttonDestructive: Color = AppColors.ButtonDestructive,
    val buttonDisabled: Color = AppColors.ButtonDisabled,
    val inputBackground: Color = AppColors.InputBackground,
    val inputBorder: Color = AppColors.InputBorder,
    val inputFocusedBorder: Color = AppColors.InputFocusedBorder
)

private val LocalAltSendmeColors = staticCompositionLocalOf { AltSendmeColors() }

/**
 * Material3 dark color scheme adapted for AltSendme
 */
private val DarkColorScheme = darkColorScheme(
    primary = AppColors.PrimaryBright,
    onPrimary = AppColors.TextPrimary,
    secondary = AppColors.AccentLight,
    onSecondary = AppColors.TextPrimary,
    tertiary = AppColors.Accent,
    onTertiary = AppColors.TextPrimary,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.Accent,
    onSurfaceVariant = AppColors.TextSecondary,
    error = AppColors.Destructive,
    onError = AppColors.TextPrimary
)

@Composable
fun AltSendmeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Always use dark theme to match desktop app
    val colors = AltSendmeColors()

    CompositionLocalProvider(LocalAltSendmeColors provides colors) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Access to custom AltSendme colors
 */
object AltSendmeTheme {
    val colors: AltSendmeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAltSendmeColors.current
}
