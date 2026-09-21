package com.monnhuitne.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Même palette que la PWA (app/src/app.css) pour une identité visuelle cohérente.
val Background = Color(0xFF0B0E14)
val SurfaceElevated = Color(0xFF12161F)
val SurfaceCard = Color(0xFF161B26)
val Border = Color(0xFF232938)
val TextPrimary = Color(0xFFE6E9F0)
val TextMuted = Color(0xFF8B93A7)
val Accent = Color(0xFF5B8CFF)
val Success = Color(0xFF3ECF8E)
val ErrorColor = Color(0xFFFF5C72)
val Warning = Color(0xFFF5B942)

private val DarkColors = darkColorScheme(
    background = Background,
    surface = SurfaceCard,
    surfaceVariant = SurfaceCard,
    primary = Accent,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onPrimary = Color.White,
    error = ErrorColor,
    outline = Border
)

@Composable
fun MonNhuitNeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
