package com.androidoctor.ui.screens.diagnose

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidoctor.ui.components.DocScreen
import com.androidoctor.ui.components.LoadingBlock
import com.androidoctor.ui.components.PrimaryButton
import com.androidoctor.ui.components.ScoreCard
import com.androidoctor.ui.components.SecondaryButton
import com.androidoctor.ui.components.VerdictCard
import com.androidoctor.ui.theme.DoctorTheme

@Composable
fun DiagnoseScreen(
    onBack: () -> Unit,
    viewModel: DiagnoseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        if (state.result == null && !state.isLoading) viewModel.runDiagnosis()
    }
    LaunchedEffect(state.shareUri) {
        state.shareUri?.let { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching {
                ctx.startActivity(Intent.createChooser(intent, "Share report").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            }.onFailure { Toast.makeText(ctx, "No app can share images", Toast.LENGTH_SHORT).show() }
            viewModel.consumeShareUri()
        }
    }
    LaunchedEffect(state.shareError) {
        state.shareError?.let {
            Toast.makeText(ctx, "Share failed: $it", Toast.LENGTH_LONG).show()
            viewModel.consumeShareError()
        }
    }

    DocScreen("Full Diagnosis", onBack) {
        val result = state.result
        when {
            state.isLoading -> LoadingBlock(state.currentStep)
            state.error != null -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text("Diagnosis failed", color = DoctorTheme.colors.danger, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(state.error!!, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Try again", onClick = { viewModel.runDiagnosis() }, modifier = Modifier.fillMaxWidth())
            }
            result != null -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).navigationBarsPadding(),
            ) {
                Text("${result.device.brand} ${result.device.model}", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text(
                    "Android ${result.device.androidVersion} · ${result.device.chipset} · ${result.device.totalRamMb} MB · ${result.device.storageType}",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                VerdictCard(
                    overallScore = result.verdict.overallScore,
                    severityLabel = result.verdict.overallSeverity.name,
                    hardwarePct = result.verdict.hardwarePct,
                    softwarePct = result.verdict.softwarePct,
                    thermalPct = result.verdict.thermalPct,
                    topIssues = result.verdict.topIssues,
                    recommendation = result.verdict.recommendation,
                )
                Spacer(Modifier.height(12.dp))
                ScoreCard("Battery and thermal", result.battery.score, result.battery.severity, result.battery.findings)
                Spacer(Modifier.height(12.dp))
                ScoreCard("Storage health", result.storage.score, result.storage.severity, result.storage.findings)
                Spacer(Modifier.height(12.dp))
                ScoreCard("Memory (RAM)", result.memory.score, result.memory.severity, result.memory.findings)
                Spacer(Modifier.height(12.dp))
                ScoreCard("CPU and processes", result.cpu.score, result.cpu.severity, result.cpu.findings)
                Spacer(Modifier.height(12.dp))
                ScoreCard("Bloatware", result.bloatware.score, result.bloatware.severity, result.bloatware.findings)
                Spacer(Modifier.height(16.dp))
                SecondaryButton(
                    if (state.sharing) "Rendering..." else "Share report card",
                    onClick = { viewModel.share() },
                    enabled = !state.sharing,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
