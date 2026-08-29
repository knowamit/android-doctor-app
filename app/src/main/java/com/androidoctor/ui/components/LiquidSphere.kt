package com.androidoctor.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Hue drifts copper (~28) to green (~142) as the fake scan progresses, so the
// orb visually "resolves" toward a good result instead of staying neutral.
private fun hueForProgress(progress: Int): Float = 28f + (142f - 28f) * (progress / 100f)

@Composable
fun LiquidSphere(
    progress: Int,
    modifier: Modifier = Modifier,
) {
    val hue = hueForProgress(progress)
    val baseColor = Color.hsv(hue, 0.75f, 0.62f)
    val baseColorDark = Color.hsv(hue + 18f, 0.7f, 0.46f)

    val infinite = rememberInfiniteTransition(label = "sphere")
    val floatOffset by infinite.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float",
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val wave1Rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "wave1",
    )
    val wave2Rotation by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "wave2",
    )
    val animatedProgress by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = tween(180, easing = LinearEasing),
        label = "progress",
    )

    Box(
        modifier = modifier
            .size(180.dp)
            .offset(y = floatOffset.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Ambient outer glow
        Box(
            modifier = Modifier
                .size(210.dp)
                .blur(28.dp)
                .background(
                    Brush.radialGradient(listOf(baseColor.copy(alpha = glowPulse * 0.6f), Color.Transparent)),
                    CircleShape,
                ),
        )

        Canvas(modifier = Modifier.size(172.dp)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val circlePath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        center - Offset(radius, radius),
                        androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    ),
                )
            }

            drawCircle(color = Color(0xFF0C0C0E), radius = radius, center = center)

            clipPath(circlePath) {
                val level = size.height * (1f - animatedProgress)
                val blobRadius = size.width * 0.95f

                rotate(degrees = wave1Rotation, pivot = Offset(center.x, level)) {
                    drawCircle(
                        brush = Brush.linearGradient(listOf(baseColor, baseColorDark)),
                        radius = blobRadius,
                        center = Offset(center.x, level),
                    )
                }
                rotate(degrees = wave2Rotation, pivot = Offset(center.x, level + 6.dp.toPx())) {
                    drawCircle(
                        color = baseColor.copy(alpha = 0.5f),
                        radius = blobRadius * 0.92f,
                        center = Offset(center.x, level + 6.dp.toPx()),
                    )
                }
            }

            // Vignette for depth
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
                blendMode = BlendMode.Multiply,
            )

            // Glassy specular highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.75f), Color.Transparent),
                    center = Offset(center.x - radius * 0.4f, center.y - radius * 0.5f),
                    radius = radius * 0.9f,
                ),
                radius = radius,
                center = center,
                blendMode = BlendMode.Screen,
            )
        }

        Text(
            text = "$progress%",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}
