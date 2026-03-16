package com.pocketclaw.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

val CrabOrange = Color(0xFFFF6B35)
val CrabOrangeDark = Color(0xFFCC5529)
val CrabOrangeLight = Color(0xFFFF8F5E)

// ── Dark palette ──
val DarkSurface = Color(0xFF0D0D0D)
val DarkCard = Color(0xFF1A1A1A)
val DarkElevated = Color(0xFF252525)
val DarkTextPrimary = Color(0xFFEEEEEE)
val DarkTextSecondary = Color(0xFF888888)
val DarkTextMuted = Color(0xFF555555)

// ── Light palette ──
val LightSurface = Color(0xFFF5F5F5)
val LightCard = Color(0xFFFFFFFF)
val LightElevated = Color(0xFFEEEEEE)
val LightTextPrimary = Color(0xFF1A1A1A)
val LightTextSecondary = Color(0xFF666666)
val LightTextMuted = Color(0xFFAAAAAA)

// ── Accent colors ──
val AccentGreen = Color(0xFF4CAF50)
val AccentBlue = Color(0xFF42A5F5)
val AccentPurple = Color(0xFFAB47BC)
val AccentYellow = Color(0xFFFFCA28)
val AccentRed = Color(0xFFEF5350)

// ── Runtime-resolved colors (used by screens) ──
object AppColors {
    val surface: Color @Composable @ReadOnlyComposable get() =
        if (LocalDarkTheme.current) DarkSurface else LightSurface
    val card: Color @Composable @ReadOnlyComposable get() =
        if (LocalDarkTheme.current) DarkCard else LightCard
    val elevated: Color @Composable @ReadOnlyComposable get() =
        if (LocalDarkTheme.current) DarkElevated else LightElevated
    val textPrimary: Color @Composable @ReadOnlyComposable get() =
        if (LocalDarkTheme.current) DarkTextPrimary else LightTextPrimary
    val textSecondary: Color @Composable @ReadOnlyComposable get() =
        if (LocalDarkTheme.current) DarkTextSecondary else LightTextSecondary
    val textMuted: Color @Composable @ReadOnlyComposable get() =
        if (LocalDarkTheme.current) DarkTextMuted else LightTextMuted
    val userBubble: Color @Composable @ReadOnlyComposable get() = CrabOrange
    val userBubbleText: Color @Composable @ReadOnlyComposable get() =
        if (LocalDarkTheme.current) DarkSurface else LightSurface
}

// Legacy aliases (still used in some screens)
val SurfaceDark = DarkSurface
val SurfaceCard = DarkCard
val SurfaceElevated = DarkElevated
val TextPrimary = DarkTextPrimary
val TextSecondary = DarkTextSecondary
val TextMuted = DarkTextMuted
