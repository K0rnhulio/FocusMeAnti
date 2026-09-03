package com.focusme.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF080C14)
val CardDark = Color(0xFF0F172A)
val CardInner = Color(0xFF1E293B)
val AccentIndigo = Color(0xFF6366F1)
val AccentCyan = Color(0xFF38BDF8)
val AccentEmerald = Color(0xFF10B981)
val AccentAmber = Color(0xFFF59E0B)
val AccentRose = Color(0xFFEF4444)
val TextMain = Color(0xFFF8FAFC)
val TextMuted = Color(0xFF94A3B8)
val TextDim = Color(0xFF64748B)

private val DarkColorScheme = darkColorScheme(
    primary = AccentIndigo,
    secondary = AccentCyan,
    tertiary = AccentEmerald,
    background = BgDark,
    surface = CardDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextMain,
    onSurface = TextMain
)

@Composable
fun FocusMeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
