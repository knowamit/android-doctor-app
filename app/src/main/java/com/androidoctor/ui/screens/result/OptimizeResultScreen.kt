package com.androidoctor.ui.screens.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build
import androidx.compose.runtime.remember
import com.androidoctor.domain.usecase.OptimizeMode
import com.androidoctor.domain.usecase.OptimizeResult
import com.androidoctor.ui.components.CircleCheck
import com.androidoctor.ui.components.DocCard
import com.androidoctor.ui.components.DocScreen
import com.androidoctor.ui.components.PrimaryButton
import com.androidoctor.ui.components.formatBytes
import com.androidoctor.ui.theme.DoctorTheme

data class ResultRow(val label: String, val value: String)

/** Rows shown on the result screen. Only measured values appear; unmeasured rows are hidden. */
fun resultRows(r: OptimizeResult, sdkInt: Int): List<ResultRow> {
    val rows = mutableListOf<ResultRow>()
    val memMeasured = r.memBeforeBytes > 0 || r.memAfterBytes > 0
    when (r.mode) {
        OptimizeMode.STANDARD -> {
            if (memMeasured) {
                rows += ResultRow("Free memory before", formatBytes(r.memBeforeBytes))
                rows += ResultRow("Free memory after", formatBytes(r.memAfterBytes))
            }
            rows += ResultRow("Temporary files cleared", formatBytes(r.storageFreedBytes))
            if (sdkInt < 34 && r.appsSignalled > 0) {
                rows += ResultRow("Apps asked to release memory", r.appsSignalled.toString())
            }
        }
        OptimizeMode.DEEP -> {
            rows += ResultRow("Storage freed", formatBytes(r.storageFreedBytes))
            if (memMeasured) {
                rows += ResultRow("Free memory before", formatBytes(r.memBeforeBytes))
                rows += ResultRow("Free memory after", formatBytes(r.memAfterBytes))
            }
            if (r.processesBefore > 0) rows += ResultRow("Processes ended", r.processesStopped.toString())
            rows += ResultRow("Settings changed", r.settingsChanged.toString())
        }
    }
    return rows
}

@Composable
fun OptimizeResultScreen(
    result: OptimizeResult,
    onDone: () -> Unit,
    onBackToHome: () -> Unit,
    onHistory: () -> Unit,
) {
    val green = DoctorTheme.colors.success
    val rows = remember(result) { resultRows(result, Build.VERSION.SDK_INT) }
    val deep = result.mode == OptimizeMode.DEEP
    DocScreen(title = "Optimization Complete", onBack = onDone) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircleCheck(green, size = 88.dp)
            Spacer(Modifier.height(16.dp))
            Text("Done", style = MaterialTheme.typography.headlineMedium)
            Text(
                "These numbers were measured on your phone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            DocCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    rows.forEach { Line(it.label, it.value) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (deep) {
                    "Settings changes can be undone from Fix History. Cleared caches are rebuilt by apps when needed."
                } else {
                    "Cleared this app's temporary files and asked Android to free memory. Your photos, messages and other app data were not touched. Memory use changes again as you open apps."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            if (!deep && Build.VERSION.SDK_INT >= 34) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Android 14 and newer do not let apps stop other apps, so this step is skipped.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Back to Home", onClick = onBackToHome, modifier = Modifier.fillMaxWidth())
            if (deep) {
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.TextButton(onClick = onHistory) { Text("View Fix History") }
            }
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    }
}
