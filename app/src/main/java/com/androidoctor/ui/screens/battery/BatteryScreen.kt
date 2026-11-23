package com.androidoctor.ui.screens.battery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidoctor.domain.model.BatteryAttribution
import com.androidoctor.ui.components.DocCard
import com.androidoctor.ui.components.DocScreen
import com.androidoctor.ui.components.EmptyNote
import com.androidoctor.ui.components.ScoreRing
import com.androidoctor.ui.theme.DoctorTheme

@Composable
fun BatteryScreen(
    onBack: () -> Unit,
    onShizukuSetup: () -> Unit = {},
    viewModel: BatteryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        if (!state.isLoading && state.diagnosis == null) viewModel.analyze()
    }

    DocScreen("Battery", onBack) {
        val diag = state.diagnosis
        when {
            state.isLoading || (diag == null && state.error == null) ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            diag == null -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyNote("Could not read battery data.")
                TextButton(onClick = viewModel::analyze) { Text("Try again") }
            }
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ScoreRing(diag.score, size = 200.dp, caption = "Battery health")
                Spacer(Modifier.height(8.dp))
                diag.findings.take(3).forEach {
                    Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(16.dp))

                DocCard(Modifier.fillMaxWidth()) {
                    Column {
                        Text("Details", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        if (diag.healthPct > 0) DetailRow("Capacity", "%.1f%%".format(diag.healthPct))
                        if (diag.cycleCount >= 0) DetailRow("Charge cycles", "${diag.cycleCount}")
                        if (diag.temperatureC > 0) DetailRow("Temperature", "%.1f°C".format(diag.temperatureC))
                        DetailRow("Throttling", if (diag.isThrottling) "Active" else "None")
                    }
                }
                Spacer(Modifier.height(12.dp))

                state.attribution?.let { AttributionCard(it, onShizukuSetup) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AttributionCard(a: BatteryAttribution, onShizukuSetup: () -> Unit) {
    DocCard(Modifier.fillMaxWidth()) {
        Column {
            Text("Battery use by app", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            when {
                a.source == BatteryAttribution.Source.UNAVAILABLE -> {
                    Text(
                        "Per-app data needs Shizuku. Setup is one time and needs no root.",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onShizukuSetup) { Text("Connect Shizuku") }
                }
                a.apps.isEmpty() -> Text(
                    "Data appears after a few hours of use.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> a.apps.take(8).forEach { app ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(app.appLabel, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1)
                            Text("%.1f%%".format(app.percentOfTotal), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (app.percentOfTotal / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = DoctorTheme.colors.track,
                            drawStopIndicator = {},
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
