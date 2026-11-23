package com.androidoctor.ui.screens.storage

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.clickable
import com.androidoctor.data.storage.AppStorage
import com.androidoctor.data.storage.unusedLabel
import com.androidoctor.ui.components.openAppSettings
import com.androidoctor.ui.components.openSystem
import com.androidoctor.ui.components.DocCard
import com.androidoctor.ui.components.DocScreen
import com.androidoctor.ui.components.PrimaryButton
import com.androidoctor.ui.components.formatBytes
import com.androidoctor.ui.theme.DoctorTheme

@Composable
fun StorageScreen(onBack: () -> Unit, onNeedShizuku: () -> Unit, viewModel: StorageViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    // Re-scan when returning from the usage access settings page.
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) viewModel.refresh() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    DocScreen("Storage Cleaner", onBack) {
        val scan = state.scan
        if (scan == null) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
            }
            return@DocScreen
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            val used = scan.totalBytes - scan.freeBytes
            DocCard(Modifier.fillMaxWidth()) {
                Column {
                    Text("${formatBytes(used)} used of ${formatBytes(scan.totalBytes)}", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { used.toFloat() / scan.totalBytes.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        trackColor = DoctorTheme.colors.track,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("${formatBytes(scan.freeBytes)} free", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))

            if (!scan.usageAccessGranted) {
                DocCard(Modifier.fillMaxWidth(), container = DoctorTheme.colors.accentContainer) {
                    Column {
                        Text("Allow usage access", fontWeight = FontWeight.SemiBold, color = DoctorTheme.colors.onAccentContainer)
                        Text(
                            "Needed to see how much cache each app holds and which apps you have not opened in a while.",
                            fontSize = 13.sp, color = DoctorTheme.colors.onAccentContainer,
                        )
                        Spacer(Modifier.height(12.dp))
                        PrimaryButton("Open settings", modifier = Modifier.fillMaxWidth(), onClick = {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        })
                    }
                }
                return@Column
            }

            DocCard(Modifier.fillMaxWidth()) {
                Column {
                    Text("App cache", fontWeight = FontWeight.SemiBold)
                    Text(formatBytes(scan.cacheBytes), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Safe to clear. Apps rebuild it when needed.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    when {
                        state.cleaning -> PrimaryButton("Cleaning...", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
                        state.shizukuReady -> PrimaryButton("Clean Now", onClick = viewModel::cleanNow, modifier = Modifier.fillMaxWidth())
                        else -> {
                            PrimaryButton("Open storage settings", modifier = Modifier.fillMaxWidth(), onClick = {
                                openSystem(context, "android.os.storage.action.MANAGE_STORAGE", Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                            })
                            androidx.compose.material3.TextButton(onClick = onNeedShizuku, modifier = Modifier.fillMaxWidth()) {
                                Text("Clean all caches with Shizuku")
                            }
                        }
                    }
                    state.freedBytes?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("Last clean freed ${formatBytes(it)} (measured)", fontSize = 13.sp, color = DoctorTheme.colors.success)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Section("Biggest caches", scan.topCacheApps, { openAppSettings(context, it.packageName) }) { formatBytes(it.cacheBytes) }
            Section("Not used in 30+ days", scan.unusedApps, { openAppSettings(context, it.packageName) }) {
                (unusedLabel(it.daysSinceUsed, true) ?: "") + " - " + formatBytes(it.totalBytes)
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun Section(title: String, apps: List<AppStorage>, onOpen: (AppStorage) -> Unit, value: (AppStorage) -> String) {
    if (apps.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp))
    DocCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp, horizontal = 16.dp)) {
        Column {
            apps.forEach { a ->
                Row(Modifier.fillMaxWidth().clickable { onOpen(a) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(a.label, Modifier.weight(1f), maxLines = 1)
                    Spacer(Modifier.width(8.dp))
                    Text(value(a), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        }
    }
}
