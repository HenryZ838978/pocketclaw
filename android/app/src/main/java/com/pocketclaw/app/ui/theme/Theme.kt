package com.pocketclaw.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import com.pocketclaw.app.data.Preferences

val LocalDarkTheme = staticCompositionLocalOf { true }

private val DarkColorScheme = darkColorScheme(
    primary = CrabOrange,
    onPrimary = DarkTextPrimary,
    primaryContainer = CrabOrangeDark,
    onPrimaryContainer = DarkTextPrimary,
    secondary = AccentBlue,
    onSecondary = DarkTextPrimary,
    tertiary = AccentPurple,
    onTertiary = DarkTextPrimary,
    background = DarkSurface,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkTextMuted,
    error = AccentRed,
    onError = DarkTextPrimary,
)

private val LightColorScheme = lightColorScheme(
    primary = CrabOrange,
    onPrimary = LightSurface,
    primaryContainer = CrabOrangeLight,
    onPrimaryContainer = LightTextPrimary,
    secondary = AccentBlue,
    onSecondary = LightSurface,
    tertiary = AccentPurple,
    onTertiary = LightSurface,
    background = LightSurface,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    outline = LightTextMuted,
    error = AccentRed,
    onError = LightSurface,
)

@Composable
fun PocketClawTheme(content: @Composable () -> Unit) {
    val themeMode = Preferences.themeMode
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    CompositionLocalProvider(LocalDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
            typography = PocketClawTypography,
            content = content,
        )
    }
}
