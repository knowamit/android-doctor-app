package com.androidoctor.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidoctor.data.shell.FixLogState
import com.androidoctor.data.shell.LineKind
import com.androidoctor.data.shell.LogLine
import com.androidoctor.data.shell.LogStep
import com.androidoctor.data.shell.StepState
import com.androidoctor.ui.theme.DoctorTheme
import kotlinx.coroutines.delay

/** Full screen live view of a fix: named steps on top, a terminal-style log below. */
@Composable
fun FixRunScreen(state: FixLogState, modifier: Modifier = Modifier) {
    val done = state.steps.count { it.state == StepState.DONE || it.state == StepState.SKIPPED || it.state == StepState.FAILED }
    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(state.title.ifBlank { "Working" }, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text("Here is exactly what is happening on your phone.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { if (state.steps.isEmpty()) 0f else done / state.steps.size.toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = DoctorTheme.colors.track,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(16.dp))
        DocCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                state.steps.forEach { StepRow(it) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Terminal(state.lines, Modifier.weight(1f).fillMaxWidth())
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StepRow(step: LogStep) {
    val c = DoctorTheme.colors
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            when (step.state) {
                StepState.RUNNING -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                StepState.DONE -> CircleCheck(c.success, size = 20.dp)
                StepState.FAILED -> Box(Modifier.size(20.dp).background(c.danger, CircleShape), contentAlignment = Alignment.Center) {
                    Text("!", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                StepState.SKIPPED -> Box(Modifier.size(20.dp).background(c.track, CircleShape), contentAlignment = Alignment.Center) {
                    Text("-", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                StepState.PENDING -> Box(Modifier.size(14.dp).background(c.track, CircleShape))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                step.title,
                fontSize = 15.sp,
                fontWeight = if (step.state == StepState.RUNNING) FontWeight.SemiBold else FontWeight.Normal,
                color = if (step.state == StepState.PENDING) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            step.summary?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun Terminal(lines: List<LogLine>, modifier: Modifier) {
    val c = DoctorTheme.colors
    val listState = rememberLazyListState()
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF070B08),
        border = BorderStroke(1.dp, c.cardBorder),
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(12.dp)) {
            itemsIndexed(lines, key = { _, l -> l.id }) { i, line ->
                val color = when (line.kind) {
                    LineKind.CMD -> Color(0xFF7BE04A)
                    LineKind.OUT -> Color(0xFF9AA79F)
                    LineKind.ERR -> Color(0xFFFF6B6B)
                    LineKind.NOTE -> Color(0xFFA48BFF)
                }
                TypedLine(line.text, color, isLast = i == lines.lastIndex)
            }
        }
    }
}

/** Types out the newest line character by character with a blinking cursor. */
@Composable
private fun TypedLine(text: String, color: Color, isLast: Boolean) {
    var shown by remember(text) { mutableIntStateOf(if (isLast) 0 else text.length) }
    LaunchedEffect(text, isLast) {
        while (shown < text.length) {
            delay(6)
            shown = (shown + 2).coerceAtMost(text.length)
        }
    }
    val blink = rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "alpha",
    )
    Row {
        Text(
            text.take(shown),
            color = color, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (isLast) Text("▌", color = color.copy(alpha = blink.value), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}
