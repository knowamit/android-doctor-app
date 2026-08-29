package com.androidoctor.ui.screens.shizuku

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidoctor.ui.theme.Copper
import com.androidoctor.ui.components.GlassShape
import com.androidoctor.ui.components.glassSurface
import com.androidoctor.ui.theme.ScoreGreen
import com.androidoctor.ui.theme.ScoreYellow
import com.androidoctor.ui.theme.Zinc100
import com.androidoctor.ui.theme.Zinc400
import com.androidoctor.ui.theme.Zinc700
import com.androidoctor.ui.theme.Zinc800
import com.androidoctor.ui.theme.Zinc900
import com.androidoctor.ui.theme.Zinc950

private const val ADB_START_CMD =
    "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh"
private const val SHIZUKU_PLAY_URL =
    "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"
private const val SHIZUKU_GUIDE_URL = "https://shizuku.rikka.app/guide/setup/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuSetupScreen(
    onBack: () -> Unit,
    viewModel: ShizukuSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SHIZUKU SETUP", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            StatusBanner(state, onRefresh = { viewModel.refresh() })
            Spacer(Modifier.height(20.dp))

            Text(
                "Shizuku gives Android Doctor privileged shell access without root. " +
                        "One-time setup. Survives reboot on Android 11+.",
                color = Zinc400,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(24.dp))

            Step(
                number = 1,
                title = "Install Shizuku from Play Store",
                complete = state.shizukuInstalled,
                action = "Open Play Store",
                onAction = {
                    runCatching {
                        ctx.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_PLAY_URL))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            )

            Step(
                number = 2,
                title = "Enable Developer Options",
                body = "Settings → About phone → tap Build number 7 times",
                action = "Open About phone",
                onAction = {
                    runCatching {
                        ctx.startActivity(
                            Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            )

            Step(
                number = 3,
                title = "Enable USB or Wireless Debugging",
                body = "Developer Options → USB debugging (or Wireless debugging on Android 11+)",
                action = "Open Developer Options",
                onAction = {
                    runCatching {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            )

            Step(
                number = 4,
                title = "Start Shizuku",
                body = "Two options — pick one:",
            ) {
                Spacer(Modifier.height(4.dp))
                Text("A) USB to computer:", color = Zinc400, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                CodeBox(ADB_START_CMD) {
                    copyToClipboard(ctx, ADB_START_CMD)
                    Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "B) Wireless (Android 11+, no PC):",
                    color = Zinc400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Open Shizuku app → Wireless debugging → Pair device → Start.",
                    color = Zinc400,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = {
                        runCatching {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_GUIDE_URL))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Copper),
                ) {
                    Text("Full guide with screenshots →", fontSize = 12.sp)
                }
            }

            Step(
                number = 5,
                title = "Verify",
                complete = state.shizukuRunning,
                body = if (state.shizukuRunning)
                    "Shizuku is running. Fix actions unlocked."
                else
                    "Tap below to check. If not running, complete steps above.",
                action = if (state.shizukuRunning) null else "Check status",
                onAction = { viewModel.refresh() },
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatusBanner(state: ShizukuSetupState, onRefresh: () -> Unit) {
    val (color, label) = when {
        state.checking -> Zinc400 to "Checking..."
        state.shizukuRunning -> ScoreGreen to "Shizuku running — all features unlocked"
        state.shizukuInstalled -> ScoreYellow to "Shizuku installed but not running"
        else -> ScoreYellow to "Shizuku not set up"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(borderColor = color)
            .padding(16.dp),
    ) {
        Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        if (!state.shizukuRunning) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Follow steps below. Takes ~5 minutes.",
                color = Zinc400,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun Step(
    number: Int,
    title: String,
    body: String? = null,
    complete: Boolean = false,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    extra: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .glassSurface(borderColor = if (complete) ScoreGreen else Zinc800)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (complete) "✓" else "$number",
                color = if (complete) ScoreGreen else Copper,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp),
            )
            Text(title, color = Zinc100, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        if (body != null) {
            Spacer(Modifier.height(6.dp))
            Text(body, color = Zinc400, fontSize = 12.sp, modifier = Modifier.padding(start = 28.dp))
        }
        if (extra != null) {
            Column(modifier = Modifier.padding(start = 28.dp)) { extra() }
        }
        if (action != null && onAction != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onAction,
                shape = GlassShape,
                modifier = Modifier
                    .padding(start = 28.dp)
                    .glassSurface(borderColor = Copper),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Copper),
                border = null,
            ) {
                Text(action, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Step(
    number: Int,
    title: String,
    body: String,
    content: @Composable () -> Unit,
) = Step(number = number, title = title, body = body, extra = content)

@Composable
private fun CodeBox(text: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(borderColor = Zinc800)
            .clickable(onClick = onCopy)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            color = Copper,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
        )
        Text("COPY", color = Zinc400, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

private fun copyToClipboard(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("adb command", text))
}
