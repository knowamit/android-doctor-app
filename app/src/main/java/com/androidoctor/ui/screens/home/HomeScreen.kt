package com.androidoctor.ui.screens.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidoctor.ui.theme.*

@Composable
fun HomeScreen(
    onDiagnose: () -> Unit,
    onFix: () -> Unit,
    onBenchmark: () -> Unit,
    onBattery: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Title — tight negative letter spacing, no gradient
        Text(
            text = "android",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Zinc100,
            letterSpacing = (-1.5).sp,
        )
        Text(
            text = "doctor",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Copper,
            letterSpacing = (-1.5).sp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Find out why your phone is slow.",
            fontSize = 14.sp,
            color = Zinc400,
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Action cards — asymmetric layout: main action full-width, rest in grid
        ActionCard(
            title = "Diagnose",
            subtitle = "Battery, storage, RAM, CPU, bloatware scan",
            icon = Icons.Outlined.Search,
            accent = Copper,
            onClick = onDiagnose,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 60/40 split — anti-AI asymmetric layout
        Row(modifier = Modifier.fillMaxWidth()) {
            ActionCard(
                title = "Fix",
                subtitle = "Debloat + optimize",
                icon = Icons.Outlined.Build,
                accent = ScoreGreen,
                onClick = onFix,
                modifier = Modifier.weight(0.6f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            ActionCard(
                title = "Battery",
                subtitle = "Drain analysis",
                icon = Icons.Outlined.BatteryAlert,
                accent = ScoreYellow,
                onClick = onBattery,
                modifier = Modifier.weight(0.4f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ActionCard(
            title = "Benchmark",
            subtitle = "App launch times, I/O speed, before/after proof",
            icon = Icons.Outlined.Speed,
            accent = CopperLight,
            onClick = onBenchmark,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))

        // Footer
        Text(
            text = "v1.0.0 · No root · No ads · No cloud",
            fontSize = 11.sp,
            color = Zinc700,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 4px radius, 1px border — anti-AI rules
    Column(
        modifier = modifier
            .border(1.dp, Zinc800, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Zinc100,
            letterSpacing = (-0.3).sp,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = Zinc400,
        )
    }
}
