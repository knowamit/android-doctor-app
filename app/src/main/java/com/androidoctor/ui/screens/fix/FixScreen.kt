package com.androidoctor.ui.screens.fix

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidoctor.domain.model.Impact
import com.androidoctor.domain.usecase.FixLevel
import com.androidoctor.ui.components.DetailRow
import com.androidoctor.ui.components.DocCard
import com.androidoctor.ui.components.openSystem
import com.androidoctor.ui.components.DocScreen
import com.androidoctor.ui.components.FixRunScreen
import com.androidoctor.ui.components.IconBadge
import com.androidoctor.ui.components.PrimaryButton
import com.androidoctor.ui.components.SectionLabel
import com.androidoctor.ui.components.SecondaryButton
import com.androidoctor.ui.components.StatusPill
import com.androidoctor.ui.theme.DoctorTheme

@Composable
fun FixScreen(
    onBack: () -> Unit,
    onShizukuSetup: () -> Unit = {},
    onHistory: () -> Unit = {},
    viewModel: FixViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val confirm = com.androidoctor.ui.components.rememberConfirmChange()
    val c = DoctorTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val log by viewModel.log.collectAsStateWithLifecycle()
    if (state.isLoading) {
        FixRunScreen(log)
        return
    }
    val ctx = androidx.compose.ui.platform.LocalContext.current

    DocScreen(
        "Fix and Optimize", onBack,
        actions = { IconButton(onClick = onHistory) { Icon(Icons.Filled.History, "Fix History") } },
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).navigationBarsPadding(),
        ) {
            state.result?.let { r ->
                SectionLabel("Fix complete")
                DocCard(Modifier.fillMaxWidth(), container = c.successContainer.copy(alpha = 0.5f)) {
                    Column {
                        DetailRow("Apps disabled", r.packagesDisabled.toString())
                        DetailRow("Settings optimized", r.settingsChanged.toString())
                        DetailRow("Battery optimizations", r.batteryOptimized.toString())
                        if (r.cacheFreedBytes > 0) DetailRow("Cache freed", "${r.cacheFreedBytes / (1024 * 1024)} MB")
                        Text("You can undo all of this from Fix History.", fontSize = 12.sp, color = muted, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            state.standardResult?.let { r ->
                SectionLabel("Quick fix complete")
                DocCard(Modifier.fillMaxWidth(), container = c.successContainer.copy(alpha = 0.5f)) {
                    Column {
                        if (r.memBeforeBytes > 0 || r.memAfterBytes > 0) {
                            DetailRow("Free memory before", "${r.memBeforeBytes / (1024 * 1024)} MB")
                            DetailRow("Free memory after", "${r.memAfterBytes / (1024 * 1024)} MB")
                        }
                        DetailRow("Temporary files cleared", "${r.cacheFreedBytes / 1024} KB")
                        if (android.os.Build.VERSION.SDK_INT < 34 && r.appsSignalled > 0) {
                            DetailRow("Apps asked to release memory", r.appsSignalled.toString())
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            state.rollbackMessage?.let { msg ->
                SectionLabel("Undo complete")
                Text(msg, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
            }

            if (!state.shizukuAvailable) {
                PrimaryButton(
                    "Quick fix",
                    onClick = { viewModel.runStandardFix() },
                    icon = Icons.Filled.Bolt,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Works on any phone. No Shizuku, no developer options. Frees memory and clears temporary files.",
                    fontSize = 12.sp, color = muted, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
                )
                SectionLabel("More you can do yourself")
                GuidedRow("Free up space", "Android's own storage manager", Icons.Filled.Storage) {
                    openSystem(ctx, "android.os.storage.action.MANAGE_STORAGE", Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                }
                Spacer(Modifier.height(8.dp))
                GuidedRow("Review all apps", "Disable or remove ones you never use", Icons.Filled.Apps) {
                    openSystem(ctx, Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)
                }
                Spacer(Modifier.height(8.dp))
                GuidedRow("Battery saver", "Limit background activity", Icons.Filled.BatteryChargingFull) {
                    openSystem(ctx, Settings.ACTION_BATTERY_SAVER_SETTINGS)
                }
                Spacer(Modifier.height(16.dp))
                DocCard(Modifier.fillMaxWidth(), container = c.accentContainer, border = c.accent.copy(alpha = 0.4f)) {
                    Column {
                        Text("Want deeper fixes? (optional)", fontWeight = FontWeight.SemiBold, color = c.onAccentContainer)
                        Text(
                            "Shizuku lets Android Doctor disable bloatware, tune animations and trim storage. It needs wireless debugging, which some banking apps dislike. Skip it if that matters to you.",
                            fontSize = 13.sp, color = c.onAccentContainer,
                        )
                        if (state.shizukuRunning) {
                            TextButton(onClick = { viewModel.requestShizukuAccess() }) { Text("Grant access") }
                        } else {
                            TextButton(onClick = onShizukuSetup) { Text("Set up Shizuku") }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            PrimaryButton(
                "Panic Boost",
                onClick = { confirm("This will turn animations off for 5 minutes, then restore them. You can undo it from Fix History.") { viewModel.runPanicBoost() } },
                enabled = state.shizukuAvailable,
                icon = Icons.Filled.Bolt,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Stops background apps, trims caches and turns animations off. Animations come back after 5 minutes.",
                fontSize = 12.sp, color = muted, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
            )
            state.panicResult?.let { p ->
                DocCard(Modifier.fillMaxWidth()) {
                    Column {
                        Text("Boost applied", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("${p.processesKilled} processes ended", fontSize = 13.sp)
                        if (p.cacheFreedBytes > 0) Text("${p.cacheFreedBytes / (1024 * 1024)} MB cache freed", fontSize = 13.sp)
                        if (p.kernelCachesDropped) Text("System caches dropped", fontSize = 13.sp)
                        if (p.animationsZeroed) Text("Animations off, back on in 5 minutes", fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // Hidden when the governor cannot be read on this phone.
            if (state.cpuGovernor != null || state.cpuBoostMessage != null) {
                SecondaryButton(
                    if (state.cpuBoostActive) "CPU boost is on (tap to turn off)" else "CPU performance boost",
                    onClick = { viewModel.toggleCpuBoost() },
                    enabled = state.shizukuAvailable && state.cpuBoostSupported,
                    color = if (state.cpuBoostActive) c.success else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.cpuBoostMessage?.let {
                    Text(it, fontSize = 12.sp, color = muted, modifier = Modifier.padding(top = 6.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Fix levels")
            PrimaryButton("Safe fix", onClick = { confirm("This will set animation speed to 0.5x. You can undo it from Fix History.") { viewModel.runFix(FixLevel.SAFE) } }, enabled = state.shizukuAvailable, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            SecondaryButton("Moderate fix and storage TRIM", onClick = { confirm("This will set animation speed to 0.5x. You can undo it from Fix History.") { viewModel.runFix(FixLevel.MODERATE) } }, enabled = state.shizukuAvailable, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            SecondaryButton("Aggressive fix and app re-optimization", onClick = { confirm("This will set animation speed to 0.5x and limit background apps to 4. You can undo it from Fix History.") { viewModel.runFix(FixLevel.AGGRESSIVE) } }, enabled = state.shizukuAvailable, modifier = Modifier.fillMaxWidth())
            if (state.hasSnapshot) {
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Undo last fix", onClick = { viewModel.rollback() }, color = c.warning, modifier = Modifier.fillMaxWidth())
            }

            if (state.bloatware.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionLabel("Removable apps found (${state.bloatware.size})")
                DocCard(Modifier.fillMaxWidth()) {
                    Column {
                        state.bloatware.take(10).forEach { e ->
                            val ic = when (e.impact) {
                                Impact.HIGH -> c.danger
                                Impact.MEDIUM -> c.warning
                                Impact.LOW -> muted
                            }
                            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                StatusPill(e.impact.name.lowercase().replaceFirstChar { it.uppercase() }, ic)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(e.name, fontSize = 14.sp)
                                    Text(e.packageName, fontSize = 11.sp, color = muted)
                                }
                            }
                        }
                        if (state.bloatware.size > 10) Text("+ ${state.bloatware.size - 10} more", fontSize = 12.sp, color = muted)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GuidedRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    DocCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
