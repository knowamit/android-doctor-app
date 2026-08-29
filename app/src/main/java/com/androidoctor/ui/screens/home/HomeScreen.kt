package com.androidoctor.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidoctor.ads.findActivity
import com.androidoctor.domain.model.QuickScanResult
import com.androidoctor.domain.model.Severity
import com.androidoctor.ui.theme.*
import com.androidoctor.ui.components.GlassShape
import com.androidoctor.ui.components.LiquidSphere
import com.androidoctor.ui.components.NativeAdCard
import com.androidoctor.ui.components.glassSurface

@Composable
fun HomeScreen(
    onDiagnose: () -> Unit,
    onFix: () -> Unit,
    onBenchmark: () -> Unit,
    onBattery: () -> Unit,
    onTrends: () -> Unit,
    onBloatware: () -> Unit,
    onHeavyApps: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is QuickScanUiState.Done) {
            context.findActivity()?.let { activity ->
                viewModel.showCompleteAd(activity) { }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
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

        Spacer(modifier = Modifier.height(24.dp))

        QuickScanBanner(uiState, onAnalyzeClick = viewModel::startScan)

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            ActionCard(
                title = "Trends",
                subtitle = "Score history over time",
                icon = Icons.Outlined.TrendingUp,
                accent = Copper,
                onClick = onTrends,
                modifier = Modifier.weight(0.5f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            ActionCard(
                title = "Bloatware",
                subtitle = "OEM catalog",
                icon = Icons.Outlined.Delete,
                accent = ScoreOrange,
                onClick = onBloatware,
                modifier = Modifier.weight(0.5f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ActionCard(
            title = "Heavy Apps",
            subtitle = "Which apps used the most time · no Shizuku needed",
            icon = Icons.Outlined.Timer,
            accent = ScoreYellow,
            onClick = onHeavyApps,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Footer
        Text(
            text = "v${com.androidoctor.BuildConfig.VERSION_NAME} · No root · No cloud",
            fontSize = 11.sp,
            color = Zinc700,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        }

        // Ad anchored to the bottom of the screen, outside the scroll area —
        // always visible regardless of scroll position.
        HorizontalDivider(color = Zinc800, thickness = 1.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Zinc950)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            NativeAdCard()
        }
    }
}

private val SCAN_LABELS = listOf(
    "Checking RAM...",
    "Checking storage...",
    "Checking battery...",
    "Reviewing running apps...",
    "Analyzing cache...",
    "Measuring thermal state...",
    "Comparing to baseline...",
    "Cross-checking history...",
    "Compiling report...",
)

@Composable
private fun QuickScanBanner(uiState: QuickScanUiState, onAnalyzeClick: () -> Unit) {
    when (uiState) {
        is QuickScanUiState.Idle -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(borderColor = Copper)
                    .clickable(onClick = onAnalyzeClick)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Analyze my phone",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Zinc100,
                    )
                    Text(
                        text = "Instant scan · no setup needed",
                        fontSize = 12.sp,
                        color = Zinc400,
                    )
                }
                Text("→", color = Copper, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        is QuickScanUiState.Scanning -> {
            var progress by remember(uiState) { mutableIntStateOf(0) }
            LaunchedEffect(uiState) {
                val start = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - start
                    progress = ((elapsed * 100) / uiState.durationMs).toInt().coerceIn(0, 99)
                    if (elapsed >= uiState.durationMs) break
                    kotlinx.coroutines.delay(80)
                }
                progress = 100
            }
            val label = SCAN_LABELS[(progress * SCAN_LABELS.size / 100).coerceIn(0, SCAN_LABELS.lastIndex)]

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(borderColor = Zinc800)
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LiquidSphere(progress = progress)
                Spacer(modifier = Modifier.height(10.dp))
                Text(label, fontSize = 12.sp, color = Zinc400)
            }
        }

        is QuickScanUiState.Done -> {
            val result = uiState.result
            val scoreColor = when {
                result.score >= 70 -> ScoreGreen
                result.score >= 40 -> ScoreYellow
                else -> ScoreRed
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(borderColor = scoreColor.copy(alpha = 0.5f))
                    .clickable(onClick = onAnalyzeClick)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${result.score}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = scoreColor,
                        letterSpacing = (-1).sp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "QUICK SCAN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Zinc400,
                            letterSpacing = 1.5.sp,
                        )
                        Text(
                            text = "RAM ${result.ramUsedPct}% · Storage ${result.storageUsedPct}%",
                            fontSize = 12.sp,
                            color = Zinc400,
                        )
                    }
                }
                if (result.findings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result.findings.first(),
                        fontSize = 12.sp,
                        color = if (result.severity == Severity.CRITICAL) ScoreRed else Zinc400,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Tap to re-scan", fontSize = 11.sp, color = Zinc700)
            }
        }
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
            .glassSurface(borderColor = Zinc800)
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
