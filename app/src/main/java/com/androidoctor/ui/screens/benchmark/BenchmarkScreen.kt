package com.androidoctor.ui.screens.benchmark

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
import com.androidoctor.domain.model.LaunchStatus
import com.androidoctor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(
    onBack: () -> Unit,
    viewModel: BenchmarkViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BENCHMARK", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Zinc950),
            )
        },
        containerColor = Zinc950,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            when {
                state.isRunning -> {
                    Spacer(modifier = Modifier.height(100.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Copper)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(state.step, color = Zinc400, fontSize = 13.sp)
                            Text("Apps will briefly open and close.", color = Zinc700, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }

                state.result != null -> {
                    val result = state.result!!
                    Spacer(modifier = Modifier.height(8.dp))

                    // System metrics
                    Text("SYSTEM METRICS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Zinc400, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Zinc800, RoundedCornerShape(4.dp))
                            .padding(16.dp),
                    ) {
                        MetricRow("RAM free", "${result.ramAvailableMb} MB")
                        MetricRow("Swap used", "${result.swapUsedMb} MB")
                        MetricRow("CPU load", "${"%.1f".format(result.cpuLoad1)}")
                        MetricRow("Processes", "${result.processCount}")
                        if (result.seqReadMbps > 0) {
                            MetricRow("Seq read", "${"%.0f".format(result.seqReadMbps)} MB/s")
                            MetricRow("Seq write", "${"%.0f".format(result.seqWriteMbps)} MB/s")
                        }
                        if (result.randReadIops > 0) {
                            MetricRow("4K rand read", "${"%.0f".format(result.randReadIops)} IOPS")
                            MetricRow("4K rand write", "${"%.0f".format(result.randWriteIops)} IOPS")
                        }
                    }

                    // App launch times
                    val okLaunches = result.appLaunches.filter { it.status == LaunchStatus.OK }.sortedByDescending { it.totalTimeMs }
                    if (okLaunches.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("APP LAUNCH TIMES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Zinc400, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        val maxMs = okLaunches.maxOf { it.totalTimeMs }
                        for (launch in okLaunches) {
                            val barFraction = launch.totalTimeMs.toFloat() / maxMs
                            val color = when {
                                launch.totalTimeMs > 2000 -> ScoreRed
                                launch.totalTimeMs > 1000 -> ScoreYellow
                                else -> ScoreGreen
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    launch.appName,
                                    fontSize = 12.sp,
                                    color = Zinc400,
                                    modifier = Modifier.width(80.dp),
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(barFraction)
                                            .height(14.dp)
                                            .border(0.dp, color, RoundedCornerShape(2.dp))
                                    )
                                }
                                Text(
                                    "${launch.totalTimeMs}ms",
                                    fontSize = 11.sp,
                                    color = Zinc300,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(60.dp).padding(start = 8.dp),
                                )
                            }
                        }

                        val avgMs = okLaunches.sumOf { it.totalTimeMs } / okLaunches.size
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Average: ${avgMs}ms across ${okLaunches.size} apps",
                            fontSize = 12.sp, color = Zinc400,
                        )
                    }
                }

                else -> {
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        "Benchmark measures real performance:",
                        fontSize = 14.sp, color = Zinc300,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    for (item in listOf("Cold app launch times", "Sequential I/O speed", "Random 4K IOPS", "RAM & swap pressure", "Process count")) {
                        Text("  · $item", fontSize = 13.sp, color = Zinc400)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Takes ~30-60 seconds.", fontSize = 12.sp, color = Zinc700)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!state.isRunning) {
                Button(
                    onClick = { viewModel.runBenchmark() },
                    colors = ButtonDefaults.buttonColors(containerColor = Copper),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Run Benchmark", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = Zinc400)
        Text(value, fontSize = 13.sp, color = Zinc100, fontWeight = FontWeight.SemiBold)
    }
}
