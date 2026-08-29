package com.androidoctor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// Spotify card radius (comfortable, 8dp — sections/dialogs scale).
val GlassShape: Shape = RoundedCornerShape(8.dp)

// Drop-in replacement for old flat `.border(...)` card wrappers: standard M3 tonal
// elevation surface, no border/glow/blur hacks.
@Composable
fun Modifier.glassSurface(
    borderColor: Color = Color.Unspecified,
    shape: Shape = GlassShape,
): Modifier = this
    .shadow(1.dp, shape)
    .clip(shape)
    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
