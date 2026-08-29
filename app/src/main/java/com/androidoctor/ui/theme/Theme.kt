package com.androidoctor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Spotify brand palette — fixed brand identity, not device dynamic color.
// Source: awesome-design-md/design-md/spotify/DESIGN.md
val Zinc950 = Color(0xFF121212) // near black — base background
val Zinc900 = Color(0xFF181818) // dark surface — cards
val Zinc800 = Color(0xFF282828) // mid card / elevated surface
val Zinc700 = Color(0xFF4D4D4D) // border gray
val Zinc400 = Color(0xFFB3B3B3) // silver — secondary text
val Zinc300 = Color(0xFFCBCBCB) // near white — secondary emphasis
val Zinc100 = Color(0xFFFFFFFF) // white — primary text

val Copper = Color(0xFF1ED760) // Spotify Green — primary accent
val CopperLight = Color(0xFF4EEA82) // lighter green tint
val CopperDark = Color(0xFF1DB954) // Spotify Green Border variant

val ScoreGreen = Color(0xFF1ED760)
val ScoreYellow = Color(0xFFFFA42B) // warning orange
val ScoreRed = Color(0xFFF3727F) // negative red
val ScoreOrange = Color(0xFFFFA42B)

private val SpotifyDarkColorScheme = darkColorScheme(
    primary = Copper,
    onPrimary = Color(0xFF000000),
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
    onError = Color(0xFF2A0A0A),
)

@Composable
fun AndroidDoctorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SpotifyDarkColorScheme,
        typography = Typography(),
        content = content,
    )
}
