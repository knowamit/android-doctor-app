package com.androidoctor.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidoctor.domain.model.Severity
import com.androidoctor.ui.theme.*

@Composable
fun ScoreCard(
    title: String,
    score: Int,
    severity: Severity,
    findings: List<String>,
    modifier: Modifier = Modifier,
) {
    val scoreColor = when {
        score >= 70 -> ScoreGreen
        score >= 40 -> ScoreYellow
        else -> ScoreRed
    }
    val severityIcon = when (severity) {
        Severity.OK -> "OK"
        Severity.WARNING -> "WARN"
        Severity.CRITICAL -> "CRIT"
    }

    // 4px radius, 1px border — anti-AI rules
    val shape = RoundedCornerShape(4.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Zinc800, shape)
            .padding(1.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
            )
            Text(
                text = "$score/100",
                color = scoreColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }

        // Score bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(4.dp)
        ) {
            // Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .border(0.dp, Color.Transparent, RoundedCornerShape(2.dp))
            )
            // Fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(score / 100f)
                    .height(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Findings
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            for (finding in findings) {
                val isWarning = finding.lowercase().let { f ->
                    listOf("critical", "high", "degraded", "elevated", "heavy").any { it in f }
                }
                Text(
                    text = finding,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        "critical" in finding.lowercase() || "severely" in finding.lowercase() -> ScoreRed
                        isWarning -> ScoreYellow
                        else -> Zinc400
                    },
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
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
    val scoreColor = when {
        overallScore >= 80 -> ScoreGreen
        overallScore >= 60 -> ScoreYellow
        overallScore >= 40 -> ScoreOrange
        else -> ScoreRed
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, scoreColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "$overallScore",
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = scoreColor,
            letterSpacing = (-2).sp,
        )
        Text(
            text = severityLabel.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = scoreColor,
            letterSpacing = 3.sp,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Root cause breakdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BreakdownItem("Hardware", hardwarePct, ScoreRed)
            BreakdownItem("Software", softwarePct, ScoreYellow)
            BreakdownItem("Thermal", thermalPct, Copper)
        }

        if (topIssues.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "TOP ISSUES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Zinc400,
                    letterSpacing = 2.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                topIssues.forEachIndexed { i, issue ->
                    Text(
                        text = "${i + 1}. $issue",
                        style = MaterialTheme.typography.bodySmall,
                        color = Zinc300,
                        modifier = Modifier.padding(vertical = 1.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = recommendation,
            style = MaterialTheme.typography.bodySmall,
            color = ScoreGreen,
        )
    }
}

@Composable
private fun BreakdownItem(label: String, pct: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$pct%",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Zinc400,
            letterSpacing = 1.sp,
        )
    }
}
