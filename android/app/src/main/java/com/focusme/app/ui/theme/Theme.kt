package com.focusme.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Deep Obsidian Palette
val BgDark = Color(0xFF060913)
val BgDarkSecondary = Color(0xFF0B101D)
val CardDark = Color(0xFF0F172A)
val CardGlass = Color(0xE6131D33)
val CardInner = Color(0xFF1E293B)
val CardBorder = Color(0x33475569)

// Vibrant Cyber & Bio Accents
val AccentIndigo = Color(0xFF6366F1)
val AccentViolet = Color(0xFF8B5CF6)
val AccentCyan = Color(0xFF06B6D4)
val AccentCyanGlow = Color(0xFF22D3EE)
val AccentEmerald = Color(0xFF10B981)
val AccentEmeraldGlow = Color(0xFF34D399)
val AccentAmber = Color(0xFFF59E0B)
val AccentRose = Color(0xFFF43F5E)

// Text Colors
val TextMain = Color(0xFFF8FAFC)
val TextMuted = Color(0xFF94A3B8)
val TextDim = Color(0xFF64748B)

// Modern Gradients
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(AccentCyan, AccentIndigo, AccentViolet)
)

val HeroCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF131D36), Color(0xFF0A0F1D))
)

val SuccessGradient = Brush.horizontalGradient(
    colors = listOf(AccentEmerald, AccentCyan)
)

val DangerGradient = Brush.horizontalGradient(
    colors = listOf(AccentRose, AccentAmber)
)

val GlassBorderGradient = Brush.linearGradient(
    colors = listOf(
        Color(0x6638BDF8),
        Color(0x226366F1),
        Color(0x11FFFFFF)
    )
)

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

fun Modifier.glassCard(
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 12.dp
): Modifier = this
    .shadow(elevation, RoundedCornerShape(cornerRadius), spotColor = AccentIndigo.copy(alpha = 0.18f))
    .clip(RoundedCornerShape(cornerRadius))
    .background(CardGlass)
    .border(1.dp, GlassBorderGradient, RoundedCornerShape(cornerRadius))

@Composable
fun FocusMeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
