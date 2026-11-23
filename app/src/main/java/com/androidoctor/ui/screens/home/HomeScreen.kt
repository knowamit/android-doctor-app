package com.androidoctor.ui.screens.home

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.res.stringResource
import com.androidoctor.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.androidoctor.ads.findActivity
import com.androidoctor.domain.usecase.OptimizeResult
import com.androidoctor.ui.components.FixRunScreen
import com.androidoctor.ui.components.NativeAdCard
import com.androidoctor.ui.components.DocCard
import com.androidoctor.ui.components.IconBadge
import com.androidoctor.ui.components.PrimaryButton
import com.androidoctor.ui.components.ScoreRing
import com.androidoctor.ui.theme.DoctorTheme

@Composable
fun HomeScreen(
    onOptimized: () -> Unit,
    onStorage: () -> Unit,
    onPerformance: () -> Unit,
    onApps: () -> Unit,
    onBattery: () -> Unit,
    onAdvanced: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val ctx = LocalContext.current
    val log by viewModel.log.collectAsState()
    val confirm = com.androidoctor.ui.components.rememberConfirmChange()
    LaunchedEffect(Unit) { viewModel.refreshShizuku() }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { e ->
                when (e) {
                    is HomeEvent.ShowResult -> onOptimized()
                }
            }
        }
    }
    if (state.optimizing) {
        FixRunScreen(log)
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Bolt, MaterialTheme.colorScheme.primary, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Settings") }
        }

        Spacer(Modifier.height(20.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.scanning && state.score == null) {
                androidx.compose.foundation.layout.Box(
                    Modifier.size(240.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            } else {
                ScoreRing(score = state.score, modifier = Modifier.padding(4.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            state.headline,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        state.error?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(20.dp))
        if (state.optimizing) {
            PrimaryButton(
                text = "Optimizing...",
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            PrimaryButton(
                text = "Optimize Now",
                icon = Icons.Filled.Bolt,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    // Only the Shizuku path changes system settings, so only it needs a confirmation.
                    if (state.shizukuReady) {
                        confirm("This will set animation speed to 0.5x. You can undo it from Fix History.") { viewModel.optimizeNow() }
                    } else {
                        viewModel.optimizeNow()
                    }
                },
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (state.shizukuReady) "Deep optimize with Shizuku. Every change can be undone." else "Works without any setup. Install Shizuku later for deeper fixes.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolTile("Clean Storage", "Free up space", Icons.Filled.Storage, Modifier.weight(1f), onStorage)
            ToolTile("Boost Performance", "Faster, smoother", Icons.Filled.RocketLaunch, Modifier.weight(1f), onPerformance)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolTile("Manage Apps", "Uninstall, debloat", Icons.Filled.Apps, Modifier.weight(1f), onApps)
            ToolTile("Battery", "Save power", Icons.Filled.BatteryChargingFull, Modifier.weight(1f), onBattery)
        }
        Spacer(Modifier.height(12.dp))
        DocCard(
            modifier = Modifier.fillMaxWidth(),
            container = DoctorTheme.colors.accentContainer,
            border = DoctorTheme.colors.accent.copy(alpha = 0.3f),
            onClick = onAdvanced,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Filled.Tune, DoctorTheme.colors.accent)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Advanced Tools", fontWeight = FontWeight.SemiBold, color = DoctorTheme.colors.onAccentContainer)
                    Text("Powered by Shizuku", fontSize = 12.sp, color = DoctorTheme.colors.onAccentContainer.copy(alpha = 0.8f))
                }
                Icon(Icons.Filled.ChevronRight, null, tint = DoctorTheme.colors.onAccentContainer)
            }
        }
        Spacer(Modifier.height(16.dp))
        NativeAdCard(Modifier.clip(RoundedCornerShape(20.dp)))
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ToolTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    DocCard(modifier = modifier, onClick = onClick) {
        Column {
            IconBadge(icon, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}
