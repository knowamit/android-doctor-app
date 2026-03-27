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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidoctor.ui.components.ScoreCard
import com.androidoctor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(
    onBack: () -> Unit,
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Zinc950),
            )
        },
        containerColor = Zinc950,
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
                            .border(1.dp, Zinc800, RoundedCornerShape(4.dp))
                            .padding(16.dp),
                    ) {
                        Text("DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Zinc400, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (diag.healthPct > 0) DetailRow("Capacity", "${"%.1f".format(diag.healthPct)}%")
                        if (diag.cycleCount >= 0) DetailRow("Charge Cycles", "${diag.cycleCount}")
                        if (diag.temperatureC > 0) DetailRow("Temperature", "${"%.1f".format(diag.temperatureC)}°C")
                        DetailRow("Throttling", if (diag.isThrottling) "Active" else "None")
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
