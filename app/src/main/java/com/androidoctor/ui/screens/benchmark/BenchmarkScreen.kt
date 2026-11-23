package com.androidoctor.ui.screens.benchmark

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidoctor.domain.model.LaunchStatus
import com.androidoctor.ui.components.DetailRow
import com.androidoctor.ui.components.DocCard
import com.androidoctor.ui.components.DocScreen
import com.androidoctor.ui.components.LoadingBlock
import com.androidoctor.ui.components.PrimaryButton
import com.androidoctor.ui.components.SectionLabel
import com.androidoctor.ui.theme.DoctorTheme

@Composable
fun BenchmarkScreen(
    onBack: () -> Unit,
    viewModel: BenchmarkViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val c = DoctorTheme.colors

    DocScreen("Benchmark", onBack) {
        if (state.isRunning) {
            LoadingBlock(state.step, sub = "Apps will briefly open and close.")
            return@DocScreen
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).navigationBarsPadding(),
        ) {
            val result = state.result
            if (result == null) {
                DocCard(Modifier.fillMaxWidth()) {
                    Column {
                        Text("Measures real performance", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        listOf("Cold app launch times", "Sequential storage speed", "Random 4K IOPS", "Memory and swap pressure", "Process count").forEach {
                            Text("• $it", fontSize = 14.sp, color = muted, modifier = Modifier.padding(vertical = 2.dp))
                        }
                        Text("Takes about 30 to 60 seconds.", fontSize = 12.sp, color = muted, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            } else {
                SectionLabel("System metrics")
                DocCard(Modifier.fillMaxWidth()) {
                    Column {
                        DetailRow("RAM free", "${result.ramAvailableMb} MB")
                        DetailRow("Swap used", "${result.swapUsedMb} MB")
                        DetailRow("CPU load", "%.1f".format(result.cpuLoad1))
                        DetailRow("Processes", "${result.processCount}")
                        if (result.seqReadMbps > 0) {
                            DetailRow("Sequential read", "%.0f MB/s".format(result.seqReadMbps))
                            DetailRow("Sequential write", "%.0f MB/s".format(result.seqWriteMbps))
                        }
                        if (result.randReadIops > 0) {
                            DetailRow("4K random read", "%.0f IOPS".format(result.randReadIops))
                            DetailRow("4K random write", "%.0f IOPS".format(result.randWriteIops))
                        }
                    }
                }
                val ok = result.appLaunches.filter { it.status == LaunchStatus.OK }.sortedByDescending { it.totalTimeMs }
                if (ok.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("App launch times")
                    DocCard(Modifier.fillMaxWidth()) {
                        Column {
                            val maxMs = ok.maxOf { it.totalTimeMs }.coerceAtLeast(1)
                            ok.forEach { l ->
                                val color = when {
                                    l.totalTimeMs > 2000 -> c.danger
                                    l.totalTimeMs > 1000 -> c.warning
                                    else -> c.success
                                }
                                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(l.appName, fontSize = 13.sp, color = muted, maxLines = 1, modifier = Modifier.width(84.dp))
                                    LinearProgressIndicator(
                                        progress = { l.totalTimeMs.toFloat() / maxMs },
                                        modifier = Modifier.weight(1f).height(8.dp),
                                        color = color, trackColor = c.track, drawStopIndicator = {},
                                    )
                                    Text("${l.totalTimeMs} ms", fontSize = 12.sp, modifier = Modifier.width(68.dp).padding(start = 8.dp))
                                }
                            }
                            val avg = ok.sumOf { it.totalTimeMs } / ok.size
                            Text("Average ${avg} ms across ${ok.size} apps", fontSize = 12.sp, color = muted, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(if (result == null) "Run benchmark" else "Run again", onClick = { viewModel.runBenchmark() }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }
    }
}
