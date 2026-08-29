package com.androidoctor.ui.screens.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidoctor.data.local.entity.ScoreSnapshotEntity
import com.androidoctor.ui.components.GlassShape
import com.androidoctor.ui.components.glassSurface
import com.androidoctor.ui.theme.Copper
import com.androidoctor.ui.theme.Zinc400
import com.androidoctor.ui.theme.Zinc700
import com.androidoctor.ui.theme.Zinc800
import com.androidoctor.ui.theme.Zinc950

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    onBack: () -> Unit,
    viewModel: TrendsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TRENDS", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            RangeSelector(state.range) { viewModel.selectRange(it) }
            Spacer(Modifier.height(24.dp))
            when {
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Copper)
                }
                state.snapshots.isEmpty() -> EmptyState()
                state.snapshots.size == 1 -> SinglePointState(state.snapshots.first().overall)
                else -> TrendChart(state.snapshots)
            }
        }
    }
}

@Composable
private fun RangeSelector(current: TrendRange, onSelect: (TrendRange) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TrendRange.entries.forEach { range ->
            val selected = range == current
            OutlinedButton(
                onClick = { onSelect(range) },
                shape = GlassShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selected) Copper else Zinc950,
                    contentColor = if (selected) Zinc950 else Zinc400,
                ),
                border = null,
                modifier = Modifier
                    .glassSurface(borderColor = if (selected) Copper else Zinc700),
            ) {
                Text(range.label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("No history yet", color = Zinc400, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Run a diagnose. Come back tomorrow for your first trend.",
            color = Zinc700,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun SinglePointState(score: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("$score", color = Copper, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Text("First snapshot", color = Zinc400, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Text("Trend appears after 3+ diagnoses", color = Zinc700, fontSize = 12.sp)
    }
}

@Composable
private fun TrendChart(snapshots: List<ScoreSnapshotEntity>) {
    val scores = snapshots.map { it.overall }
    val minScore = (scores.min() - 5).coerceAtLeast(0)
    val maxScore = (scores.max() + 5).coerceAtMost(100)
    val range = (maxScore - minScore).coerceAtLeast(1)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${scores.last()}", color = Copper, fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
            Spacer(Modifier.width(12.dp))
            val delta = scores.last() - scores.first()
            val arrow = if (delta >= 0) "↗" else "↘"
            Text(
                "$arrow ${if (delta >= 0) "+" else ""}$delta pts",
                color = if (delta >= 0) Copper else Zinc400,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text("Overall score · ${snapshots.size} snapshots", color = Zinc700, fontSize = 12.sp)
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(GlassShape)
                .glassSurface(borderColor = Zinc800)
                .padding(16.dp),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (scores.size < 2) return@Canvas
                val dx = w / (scores.size - 1).coerceAtLeast(1)
                val points = scores.mapIndexed { i, s ->
                    val x = i * dx
                    val y = h - ((s - minScore).toFloat() / range) * h
                    Offset(x, y)
                }
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path = path, color = Copper, style = Stroke(width = 3f))
                points.forEach { drawCircle(Copper, radius = 4f, center = it) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("min $minScore", color = Zinc700, fontSize = 11.sp)
            Text("max $maxScore", color = Zinc700, fontSize = 11.sp)
        }
    }
}
