package com.androidoctor.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Anti-AI UI: copper accent, zinc-950 background, no purple/blue gradients
val Zinc950 = Color(0xFF09090B)
val Zinc900 = Color(0xFF18181B)
val Zinc800 = Color(0xFF27272A)
val Zinc700 = Color(0xFF3F3F46)
val Zinc400 = Color(0xFFA1A1AA)
val Zinc300 = Color(0xFFD4D4D8)
val Zinc100 = Color(0xFFF4F4F5)
val Copper = Color(0xFFC2703E)
val CopperLight = Color(0xFFE8956A)
val CopperDark = Color(0xFF8B4F2A)

val ScoreGreen = Color(0xFF22C55E)
val ScoreYellow = Color(0xFFEAB308)
val ScoreRed = Color(0xFFEF4444)
val ScoreOrange = Color(0xFFF97316)

private val DarkColorScheme = darkColorScheme(
    primary = Copper,
    onPrimary = Zinc950,
    primaryContainer = CopperDark,
    onPrimaryContainer = CopperLight,
    secondary = Zinc400,
    onSecondary = Zinc950,
    secondaryContainer = Zinc800,
    onSecondaryContainer = Zinc300,
    background = Zinc950,
    onBackground = Zinc100,
    surface = Zinc900,
    onSurface = Zinc100,
    surfaceVariant = Zinc800,
    onSurfaceVariant = Zinc400,
    outline = Zinc700,
    outlineVariant = Zinc800,
    error = ScoreRed,
)

@Composable
fun AndroidDoctorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content,
    )
}
