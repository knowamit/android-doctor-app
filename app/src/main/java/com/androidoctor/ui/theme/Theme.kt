package com.androidoctor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Legacy tokens. Screens that have not been migrated to the new design still
// read these directly, so they are wrapped in ForceDarkTheme (see below).
// ---------------------------------------------------------------------------
val Zinc950 = Color(0xFF121212)
val Zinc900 = Color(0xFF181818)
val Zinc800 = Color(0xFF282828)
val Zinc700 = Color(0xFF4D4D4D)
val Zinc400 = Color(0xFFB3B3B3)
val Zinc300 = Color(0xFFCBCBCB)
val Zinc100 = Color(0xFFFFFFFF)

val Copper = Color(0xFF1ED760)
val CopperLight = Color(0xFF4EEA82)
val CopperDark = Color(0xFF1DB954)

val ScoreGreen = Color(0xFF1ED760)
val ScoreYellow = Color(0xFFFFA42B)
val ScoreRed = Color(0xFFF3727F)
val ScoreOrange = Color(0xFFFFA42B)

// ---------------------------------------------------------------------------
// New design: lime green primary, violet for Shizuku / advanced tools.
// ---------------------------------------------------------------------------
private val Lime = Color(0xFF7BE04A)
private val LimeDeep = Color(0xFF3FAE22)
private val Violet = Color(0xFF7B5CF0)
private val VioletSoft = Color(0xFFEDE8FF)

@Immutable
data class DoctorColors(
    val success: Color,
    val warning: Color,
    val danger: Color,
    val accent: Color,          // violet
    val accentContainer: Color, // soft violet card
    val onAccentContainer: Color,
    val track: Color,           // progress / ring track
    val cardBorder: Color,
    val dangerContainer: Color,
    val successContainer: Color,
)

private val DarkDoctorColors = DoctorColors(
    success = Lime,
    warning = Color(0xFFFFB84D),
    danger = Color(0xFFFF6B6B),
    accent = Color(0xFFA48BFF),
    accentContainer = Color(0xFF241E3F),
    onAccentContainer = Color(0xFFD8CFFF),
    track = Color(0xFF26302B),
    cardBorder = Color(0xFF232B27),
    dangerContainer = Color(0xFF3A1E1E),
    successContainer = Color(0xFF16301A),
)

private val LightDoctorColors = DoctorColors(
    success = LimeDeep,
    warning = Color(0xFFE08A00),
    danger = Color(0xFFD93A3A),
    accent = Violet,
    accentContainer = VioletSoft,
    onAccentContainer = Color(0xFF3B2A94),
    track = Color(0xFFE6EAE7),
    cardBorder = Color(0xFFE4E8E5),
    dangerContainer = Color(0xFFFDEBEA),
    successContainer = Color(0xFFE6F6DD),
)

val LocalDoctorColors = staticCompositionLocalOf { DarkDoctorColors }

object DoctorTheme {
    val colors: DoctorColors
        @Composable get() = LocalDoctorColors.current
}

private val DarkScheme = darkColorScheme(
    primary = Lime,
    onPrimary = Color(0xFF07150A),
    primaryContainer = Color(0xFF1F4A12),
    onPrimaryContainer = Color(0xFFCFF5BC),
    secondary = Color(0xFFA48BFF),
    onSecondary = Color(0xFF1B1240),
    secondaryContainer = Color(0xFF241E3F),
    onSecondaryContainer = Color(0xFFD8CFFF),
    background = Color(0xFF080C0A),
    onBackground = Color(0xFFF2F5F3),
    surface = Color(0xFF111713),
    onSurface = Color(0xFFF2F5F3),
    surfaceVariant = Color(0xFF1A221D),
    onSurfaceVariant = Color(0xFF9AA79F),
    outline = Color(0xFF2C3630),
    outlineVariant = Color(0xFF232B27),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2A0A0A),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF4FC22E),
    onPrimary = Color(0xFF05140A),
    primaryContainer = Color(0xFFD8F5C8),
    onPrimaryContainer = Color(0xFF123A06),
    secondary = Violet,
    onSecondary = Color.White,
    secondaryContainer = VioletSoft,
    onSecondaryContainer = Color(0xFF3B2A94),
    background = Color(0xFFF6F8F5),
    onBackground = Color(0xFF0E130F),
    surface = Color.White,
    onSurface = Color(0xFF0E130F),
    surfaceVariant = Color(0xFFEEF2EE),
    onSurfaceVariant = Color(0xFF5B665F),
    outline = Color(0xFFD5DBD6),
    outlineVariant = Color(0xFFE4E8E5),
    error = Color(0xFFD93A3A),
    onError = Color.White,
)

private val DoctorTypography = Typography(
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun AndroidDoctorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalDoctorColors provides if (darkTheme) DarkDoctorColors else LightDoctorColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = DoctorTypography,
            content = content,
        )
    }
}

/** Wraps screens that still use the legacy fixed dark palette. */
@Composable
fun ForceDarkTheme(content: @Composable () -> Unit) {
    AndroidDoctorTheme(darkTheme = true, content = content)
}
