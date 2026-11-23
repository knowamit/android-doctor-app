package com.androidoctor.ui.screens.usage

import com.androidoctor.R
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidoctor.data.parser.HeavyApp
import com.androidoctor.ui.components.DocCard
import com.androidoctor.ui.components.DocScreen
import com.androidoctor.ui.components.EmptyNote
import com.androidoctor.ui.components.PrimaryButton
import com.androidoctor.ui.theme.DoctorTheme
import java.util.concurrent.TimeUnit

@Composable
fun HeavyAppsScreen(
    onBack: () -> Unit,
    viewModel: HeavyAppsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    DocScreen("Heavy Apps", onBack) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            !state.hasPermission -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text("Usage access needed", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.app_name) + " needs Usage Access to show which apps you spent the most time in. This data stays on your phone.",
                    color = muted, fontSize = 14.sp,
                )
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Open Usage Access settings", onClick = {
                    runCatching {
                        ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { viewModel.refresh() }) { Text("I turned it on, check again") }
            }
            state.apps.isEmpty() -> EmptyNote("No heavy apps yet. Come back after a few days of use.")
            else -> Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Text("Most used apps in the last 7 days. Tap one to manage or force stop it.", color = muted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.navigationBarsPadding(),
                ) {
                    items(state.apps, key = { it.packageName }) { app -> HeavyAppRow(app) { openAppInfo(ctx, app.packageName) } }
                }
            }
        }
    }
}

@Composable
private fun HeavyAppRow(app: HeavyApp, onClick: () -> Unit) {
    val c = DoctorTheme.colors
    val hours = TimeUnit.MILLISECONDS.toMinutes(app.foregroundTimeMs) / 60f
    val tier = when {
        hours > 20 -> c.danger
        hours > 10 -> c.warning
        else -> c.success
    }
    val daysSince = (System.currentTimeMillis() - app.lastUsedMs) / (1000L * 60 * 60 * 24)
    DocCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(app.appLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    if (daysSince == 0L) "Used today" else "Last used $daysSince days ago",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("%.1f h".format(hours), color = tier, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("in foreground", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun openAppInfo(ctx: android.content.Context, pkg: String) {
    runCatching {
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
