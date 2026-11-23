package com.androidoctor.ui.screens.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidoctor.data.local.entity.ScoreSnapshotEntity
import com.androidoctor.ui.components.DocCard
import com.androidoctor.ui.components.DocScreen
import com.androidoctor.ui.components.EmptyNote
import com.androidoctor.ui.components.scoreColor
import com.androidoctor.ui.theme.DoctorTheme

@Composable
fun TrendsScreen(
    onBack: () -> Unit,
    viewModel: TrendsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DocScreen("Health Trends", onBack) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            RangeSelector(state.range) { viewModel.selectRange(it) }
            Spacer(Modifier.height(20.dp))
            when {
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                state.snapshots.isEmpty() -> EmptyNote("No history yet. Run a scan, then come back tomorrow for your first trend.")
                state.snapshots.size == 1 -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.snapshots.first().overall}", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = scoreColor(state.snapshots.first().overall))
                    Text("First snapshot", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    EmptyNote("The trend line appears after a few scans.")
                }
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
            Box(
                Modifier
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(50),
                    )
                    .clickable { onSelect(range) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Text(
                    range.label, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TrendChart(snapshots: List<ScoreSnapshotEntity>) {
    val scores = snapshots.map { it.overall }
    val minScore = (scores.min() - 5).coerceAtLeast(0)
    val maxScore = (scores.max() + 5).coerceAtMost(100)
    val span = (maxScore - minScore).coerceAtLeast(1)
    val lineColor = MaterialTheme.colorScheme.primary
    val c = DoctorTheme.colors
    val delta = scores.last() - scores.first()

    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${scores.last()}", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = scoreColor(scores.last()))
            Spacer(Modifier.width(12.dp))
            Text(
                "${if (delta >= 0) "+" else ""}$delta pts",
                color = if (delta >= 0) c.success else c.danger,
                fontSize = 15.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        Text("Overall score, ${snapshots.size} snapshots", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        DocCard(Modifier.fillMaxWidth().height(240.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val dx = size.width / (scores.size - 1).coerceAtLeast(1)
                val pts = scores.mapIndexed { i, s -> Offset(i * dx, size.height - ((s - minScore).toFloat() / span) * size.height) }
                val path = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, lineColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
                pts.forEach { drawCircle(lineColor, radius = 7f, center = it) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Low $minScore", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text("High $maxScore", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}
