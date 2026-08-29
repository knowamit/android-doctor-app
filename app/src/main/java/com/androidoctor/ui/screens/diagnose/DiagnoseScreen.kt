package com.androidoctor.ui.screens.diagnose

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidoctor.ui.components.ScoreCard
import com.androidoctor.ui.components.VerdictCard
import com.androidoctor.ui.theme.*
import com.androidoctor.ui.components.GlassShape
import com.androidoctor.ui.components.glassSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnoseScreen(
    onBack: () -> Unit,
    viewModel: DiagnoseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (state.result == null && !state.isLoading) {
            viewModel.runDiagnosis()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "DIAGNOSE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Copper)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            state.currentStep,
                            color = Zinc400,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error", color = ScoreRed, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.error!!, color = Zinc400, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { viewModel.runDiagnosis() },
                            shape = GlassShape,
                            modifier = Modifier.glassSurface(borderColor = ScoreRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ScoreRed),
                            border = null,
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            state.result != null -> {
                val result = state.result!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    // Device info
                    Text(
                        text = "DEVICE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Zinc400,
                        letterSpacing = 2.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${result.device.brand} ${result.device.model}",
                        fontWeight = FontWeight.SemiBold,
                        color = Zinc100,
                    )
                    Text(
                        text = "Android ${result.device.androidVersion} · ${result.device.chipset} · ${result.device.totalRamMb}MB · ${result.device.storageType}",
                        fontSize = 12.sp,
                        color = Zinc400,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Score cards
                    ScoreCard("Battery & Thermal", result.battery.score, result.battery.severity, result.battery.findings)
                    Spacer(modifier = Modifier.height(12.dp))

                    ScoreCard("Storage Health", result.storage.score, result.storage.severity, result.storage.findings)
                    Spacer(modifier = Modifier.height(12.dp))

                    ScoreCard("Memory (RAM)", result.memory.score, result.memory.severity, result.memory.findings)
                    Spacer(modifier = Modifier.height(12.dp))

                    ScoreCard("CPU & Processes", result.cpu.score, result.cpu.severity, result.cpu.findings)
                    Spacer(modifier = Modifier.height(12.dp))

                    ScoreCard("Bloatware", result.bloatware.score, result.bloatware.severity, result.bloatware.findings)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Verdict
                    VerdictCard(
                        overallScore = result.verdict.overallScore,
                        severityLabel = result.verdict.overallSeverity.name,
                        hardwarePct = result.verdict.hardwarePct,
                        softwarePct = result.verdict.softwarePct,
                        thermalPct = result.verdict.thermalPct,
                        topIssues = result.verdict.topIssues,
                        recommendation = result.verdict.recommendation,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val ctx = LocalContext.current
                    LaunchedEffect(state.shareUri) {
                        state.shareUri?.let { uri ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching {
                                ctx.startActivity(Intent.createChooser(intent, "Share report").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }.onFailure {
                                Toast.makeText(ctx, "No app can share images", Toast.LENGTH_SHORT).show()
                            }
                            viewModel.consumeShareUri()
                        }
                    }
                    LaunchedEffect(state.shareError) {
                        state.shareError?.let { err ->
                            Toast.makeText(ctx, "Share failed: $err", Toast.LENGTH_LONG).show()
                            viewModel.consumeShareError()
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.share() },
                        enabled = !state.sharing,
                        shape = GlassShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassSurface(borderColor = Copper),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Copper),
                        border = null,
                    ) {
                        Text(if (state.sharing) "Rendering..." else "Share report card", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
