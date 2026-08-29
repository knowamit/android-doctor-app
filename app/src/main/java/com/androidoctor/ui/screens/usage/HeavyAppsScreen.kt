package com.androidoctor.ui.screens.usage

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.androidoctor.data.parser.HeavyApp
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
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeavyAppsScreen(
    onBack: () -> Unit,
    viewModel: HeavyAppsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HEAVY APPS · 7d", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Copper)
                }
                !state.hasPermission -> PermissionPrompt(
                    onGrant = {
                        runCatching {
                            ctx.startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
                    onRetry = { viewModel.refresh() },
                )
                state.apps.isEmpty() -> EmptyState()
                else -> {
                    Text(
                        "Apps using the most time last 7 days. Tap to manage or force-stop.",
                        color = Zinc400, fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.apps, key = { it.packageName }) { app ->
                            HeavyAppRow(app) {
                                openAppInfo(ctx, app.packageName)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onGrant: () -> Unit, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Usage access needed", color = ScoreYellow, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Android Doctor needs Usage Access to show which apps used the most time. " +
                    "No data leaves your device.",
            color = Zinc400, fontSize = 13.sp,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onGrant,
            shape = GlassShape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Copper),
            modifier = Modifier.glassSurface(borderColor = Copper),
            border = null,
        ) {
            Text("Open Usage Access Settings", fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Text("I enabled it, recheck", color = Zinc400, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No heavy apps yet", color = Zinc400, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text("Come back after a few days of use.", color = Zinc700, fontSize = 12.sp)
    }
}

@Composable
private fun HeavyAppRow(app: HeavyApp, onClick: () -> Unit) {
    val hours = TimeUnit.MILLISECONDS.toMinutes(app.foregroundTimeMs) / 60f
    val tierColor = when {
        hours > 20 -> ScoreRed
        hours > 10 -> ScoreOrange
        hours > 5 -> ScoreYellow
        else -> ScoreGreen
    }
    val daysSince = (System.currentTimeMillis() - app.lastUsedMs) / (1000L * 60 * 60 * 24)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(borderColor = Zinc800)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(app.appLabel, color = Zinc100, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (daysSince == 0L) "Used today" else "Last used $daysSince days ago",
                color = Zinc700, fontSize = 10.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${"%.1f".format(hours)}h", color = tierColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("foreground", color = Zinc700, fontSize = 10.sp)
        }
    }
}

private fun openAppInfo(ctx: android.content.Context, pkg: String) {
    runCatching {
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:$pkg"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
