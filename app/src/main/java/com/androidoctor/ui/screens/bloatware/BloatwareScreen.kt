package com.androidoctor.ui.screens.bloatware

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.androidoctor.domain.model.BloatwareCatalogEntry
import com.androidoctor.ui.components.GlassShape
import com.androidoctor.ui.components.glassSurface
import com.androidoctor.ui.theme.Copper
import com.androidoctor.ui.theme.ScoreGreen
import com.androidoctor.ui.theme.ScoreOrange
import com.androidoctor.ui.theme.ScoreRed
import com.androidoctor.ui.theme.ScoreYellow
import com.androidoctor.ui.theme.Zinc100
import com.androidoctor.ui.theme.Zinc400
import com.androidoctor.ui.theme.Zinc700
import com.androidoctor.ui.theme.Zinc800
import com.androidoctor.ui.theme.Zinc950

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloatwareScreen(
    onBack: () -> Unit,
    viewModel: BloatwareViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.lastMessage) {
        state.lastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLOATWARE", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            val catalog = state.catalog
            when {
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Copper)
                }
                catalog == null || catalog.installedEntries.isEmpty() -> EmptyState(catalog?.oem)
                else -> {
                    Text(
                        "OEM: ${catalog.oem.uppercase()}",
                        fontSize = 11.sp, letterSpacing = 1.5.sp,
                        color = Zinc400, fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "${catalog.installedEntries.size} of ${catalog.entries.size} flagged apps installed",
                        fontSize = 12.sp, color = Zinc700,
                    )
                    Spacer(Modifier.height(16.dp))
                    val ctx = LocalContext.current
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(catalog.installedEntries, key = { it.packageName }) { entry ->
                            BloatwareRow(
                                entry = entry,
                                disabled = entry.packageName in state.disabled,
                                shizukuAvailable = state.shizukuAvailable,
                                onDisable = { viewModel.disable(entry.packageName) },
                                onOpenSettings = {
                                    runCatching {
                                        ctx.startActivity(
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                                .setData(Uri.parse("package:${entry.packageName}"))
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(oem: String?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Clean device", color = ScoreGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            oem?.let { "No known bloatware for $it" } ?: "No OEM detected",
            color = Zinc400, fontSize = 13.sp,
        )
    }
}

@Composable
private fun BloatwareRow(
    entry: BloatwareCatalogEntry,
    disabled: Boolean,
    shizukuAvailable: Boolean,
    onDisable: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val severityColor = when (entry.severity) {
        BloatwareCatalogEntry.Severity.light -> ScoreYellow
        BloatwareCatalogEntry.Severity.moderate -> ScoreOrange
        BloatwareCatalogEntry.Severity.heavy -> ScoreRed
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(borderColor = Zinc800)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.name, color = Zinc100, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(entry.packageName, color = Zinc700, fontSize = 10.sp)
            }
            Text(
                entry.severity.name.uppercase(),
                color = severityColor,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row {
            if (entry.ramMb > 0) Text("~${entry.ramMb}MB RAM", color = Zinc400, fontSize = 11.sp)
            if (entry.batteryPct > 0) {
                Spacer(Modifier.width(12.dp))
                Text("${entry.batteryPct}% battery", color = Zinc400, fontSize = 11.sp)
            }
        }
        if (entry.notes.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(entry.notes, color = Zinc700, fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
        if (entry.safe) {
            if (disabled) {
                Text("Disabled ✓", color = ScoreGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            } else if (shizukuAvailable) {
                TextButton(
                    onClick = onDisable,
                    modifier = Modifier.glassSurface(borderColor = Copper),
                    colors = ButtonDefaults.textButtonColors(contentColor = Copper),
                ) {
                    Text("Disable", fontSize = 12.sp)
                }
            } else {
                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.glassSurface(borderColor = Zinc700),
                    colors = ButtonDefaults.textButtonColors(contentColor = Zinc400),
                ) {
                    Text("Open in Settings →", fontSize = 12.sp)
                }
            }
        } else {
            Text("⚠ Do not disable", color = ScoreRed, fontSize = 11.sp)
        }
    }
}
