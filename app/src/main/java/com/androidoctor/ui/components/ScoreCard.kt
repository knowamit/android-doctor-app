package com.androidoctor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidoctor.domain.model.Severity
import com.androidoctor.ui.theme.DoctorTheme

/** One diagnosis section: title, score, bar and its findings. */
@Composable
fun ScoreCard(
    title: String,
    score: Int,
    severity: Severity,
    findings: List<String>,
    modifier: Modifier = Modifier,
) {
    val color = scoreColor(score)
    DocCard(modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("$score/100", color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = color,
                trackColor = DoctorTheme.colors.track,
                drawStopIndicator = {},
            )
            if (severity != Severity.OK) {
                Spacer(Modifier.height(8.dp))
                StatusPill(if (severity == Severity.CRITICAL) "Needs attention" else "Could be better", color)
            }
            Spacer(Modifier.height(8.dp))
            findings.forEach { f ->
                val lower = f.lowercase()
                val c = when {
                    "critical" in lower || "severely" in lower -> DoctorTheme.colors.danger
                    listOf("high", "degraded", "elevated", "heavy").any { it in lower } -> DoctorTheme.colors.warning
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(f, color = c, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
fun VerdictCard(
    overallScore: Int,
    severityLabel: String,
    hardwarePct: Int,
    softwarePct: Int,
    thermalPct: Int,
    topIssues: List<String>,
    recommendation: String,
    modifier: Modifier = Modifier,
) {
    val c = DoctorTheme.colors
    DocCard(modifier.fillMaxWidth(), container = c.successContainer.copy(alpha = 0.5f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ScoreRing(overallScore, size = 160.dp, caption = severityLabel.lowercase().replaceFirstChar { it.uppercase() })
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Breakdown("Hardware", hardwarePct, c.danger)
                Breakdown("Software", softwarePct, c.warning)
                Breakdown("Thermal", thermalPct, c.accent)
            }
            if (topIssues.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Column(Modifier.fillMaxWidth()) {
                    Text("Top issues", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    topIssues.forEachIndexed { i, issue ->
                        Text("${i + 1}. $issue", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(recommendation, fontSize = 13.sp, color = c.success, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun Breakdown(label: String, pct: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$pct%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
