package com.androidoctor.ui.screens.battery

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidoctor.ui.components.ScoreCard
import com.androidoctor.ui.components.ShizukuSetupLink
import com.androidoctor.ui.theme.*
import com.androidoctor.ui.components.GlassShape
import com.androidoctor.ui.components.glassSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(
    onBack: () -> Unit,
    onShizukuSetup: () -> Unit = {},
    viewModel: BatteryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (!state.isLoading && state.diagnosis == null) {
            viewModel.analyze()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BATTERY", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Copper)
                }
            }
            state.diagnosis != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ScoreCard("Battery & Thermal", state.diagnosis!!.score, state.diagnosis!!.severity, state.diagnosis!!.findings)

                    // Battery stats
                    val diag = state.diagnosis!!
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassSurface(borderColor = Zinc800)
                            .padding(16.dp),
                    ) {
                        Text("DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Zinc400, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (diag.healthPct > 0) DetailRow("Capacity", "${"%.1f".format(diag.healthPct)}%")
                        if (diag.cycleCount >= 0) DetailRow("Charge Cycles", "${diag.cycleCount}")
                        if (diag.temperatureC > 0) DetailRow("Temperature", "${"%.1f".format(diag.temperatureC)}°C")
                        DetailRow("Throttling", if (diag.isThrottling) "Active" else "None")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Per-app attribution
                    val attribution = state.attribution
                    if (attribution != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassSurface(borderColor = Zinc800)
                                .padding(16.dp),
                        ) {
                            Text("PER-APP USAGE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Zinc400, letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                when (attribution.source) {
                                    com.androidoctor.domain.model.BatteryAttribution.Source.NATIVE_API_31 -> "via system API"
                                    com.androidoctor.domain.model.BatteryAttribution.Source.SHIZUKU_DUMPSYS -> "via Shizuku dumpsys"
                                    com.androidoctor.domain.model.BatteryAttribution.Source.UNAVAILABLE -> "Requires Shizuku (setup once via ADB)"
                                },
                                fontSize = 10.sp,
                                color = Zinc700,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (attribution.source == com.androidoctor.domain.model.BatteryAttribution.Source.UNAVAILABLE) {
                                Text(
                                    "Per-app data requires Shizuku. One-time ADB setup, no root needed.",
                                    fontSize = 12.sp,
                                    color = Zinc400,
                                )
                                ShizukuSetupLink(onOpen = onShizukuSetup)
                            } else if (attribution.apps.isEmpty()) {
                                Text(
                                    "Data will appear after a few hours of use.",
                                    fontSize = 12.sp,
                                    color = Zinc400,
                                )
                            } else {
                                attribution.apps.take(8).forEach { app ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            app.appLabel,
                                            fontSize = 13.sp,
                                            color = Zinc100,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            "${"%.1f".format(app.percentOfTotal)}%",
                                            fontSize = 13.sp,
                                            color = Copper,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = Zinc400)
        Text(value, fontSize = 13.sp, color = Zinc100, fontWeight = FontWeight.SemiBold)
    }
}
