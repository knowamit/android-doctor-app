package com.androidoctor.ui.screens.shizuku

import com.androidoctor.R
import androidx.compose.ui.res.stringResource
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.androidoctor.ui.components.CircleCheck
import com.androidoctor.ui.components.DocCard
import com.androidoctor.ui.components.DocScreen
import com.androidoctor.ui.components.IconBadge
import com.androidoctor.ui.components.PrimaryButton
import com.androidoctor.ui.components.StatusPill
import com.androidoctor.ui.theme.DoctorTheme

private const val SHIZUKU_PLAY_URL = "https://play.google.com/store/apps/details?id=$SHIZUKU_PACKAGE"
private const val SHIZUKU_GUIDE_URL = "https://shizuku.rikka.app/guide/setup/"

@Composable
fun ShizukuSetupScreen(onBack: () -> Unit, viewModel: ShizukuSetupViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsState()
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    // The user leaves to install or start Shizuku, so re-check whenever they come back.
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) viewModel.refresh() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    fun open(intent: Intent?) { intent?.let { runCatching { ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } } }

    DocScreen("Shizuku", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconBadge(Icons.Filled.Usb, DoctorTheme.colors.accent, size = 80.dp)
            Spacer(Modifier.height(12.dp))
            if (s.granted) StatusPill("Connected", DoctorTheme.colors.success)
            else StatusPill("Not connected", DoctorTheme.colors.warning)
            Spacer(Modifier.height(8.dp))
            Text(
                "Shizuku lets " + stringResource(R.string.app_name) + " clean caches and disable apps without root. Commands run on your phone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(20.dp))

            StepCard(1, "Install Shizuku", "Get it from Google Play.", done = s.installed) {
                if (!s.installed) PrimaryButton("Open Google Play", onClick = {
                    open(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_PLAY_URL)))
                }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(10.dp))
            StepCard(
                2, "Start Shizuku",
                "Open Shizuku and start it with Wireless debugging (Android 11+) or from a computer.",
                done = s.running,
            ) {
                if (s.installed && !s.running) {
                    PrimaryButton("Open Shizuku", onClick = {
                        open(ctx.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE))
                    }, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { open(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_GUIDE_URL))) }) {
                        Text("Step by step guide")
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            StepCard(
                3, "Allow " + stringResource(R.string.app_name),
                when {
                    s.granted -> stringResource(R.string.app_name) + " is allowed to use Shizuku."
                    s.denied -> "Permission was denied. Allow it from the Shizuku app, under Authorized apps."
                    else -> "Tap below and accept the Shizuku dialog."
                },
                done = s.granted,
            ) {
                if (s.running && !s.granted) {
                    if (s.denied) PrimaryButton("Open Shizuku", onClick = {
                        open(ctx.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE))
                    }, modifier = Modifier.fillMaxWidth())
                    else PrimaryButton("Grant permission", onClick = viewModel::requestPermission, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepCard(
    number: Int,
    title: String,
    body: String,
    done: Boolean,
    action: @Composable () -> Unit,
) {
    DocCard(Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (done) CircleCheck(DoctorTheme.colors.success, size = 28.dp)
                else Row(
                    Modifier.size(28.dp).background(DoctorTheme.colors.accent, CircleShape),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) { Text(number.toString(), color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(body, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            action()
        }
    }
}
