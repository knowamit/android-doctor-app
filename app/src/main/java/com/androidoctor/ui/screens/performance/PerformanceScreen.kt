package com.androidoctor.ui.screens.performance

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.androidoctor.ui.components.DocCard
import com.androidoctor.ui.components.DocScreen
import com.androidoctor.ui.components.IconBadge
import com.androidoctor.ui.components.PrimaryButton
import com.androidoctor.ui.components.openDeveloperOptions

@Composable
fun PerformanceScreen(onBack: () -> Unit, onNeedShizuku: () -> Unit, viewModel: PerformanceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var devHint by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    DocScreen("Boost Performance", onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (!state.shizukuReady) {
                DocCard(Modifier.fillMaxWidth()) {
                    Column {
                        Text("These are Developer options.", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Open Developer options to change animation speed yourself, or use Shizuku to do it here.",
                            fontSize = 13.sp,
                        )
                        if (devHint) {
                            Text("Tap Build number 7 times to turn on Developer options.", fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        PrimaryButton("Open Developer options", onClick = { devHint = !openDeveloperOptions(ctx) }, modifier = Modifier.fillMaxWidth())
                        androidx.compose.material3.TextButton(onClick = onNeedShizuku, modifier = Modifier.fillMaxWidth()) {
                            Text("Use Shizuku instead")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            ToggleRow(
                Icons.Filled.Animation, "Reduce animations",
                "Halves animation time so the phone feels quicker.",
                state.reduceAnimations, state.shizukuReady && !state.busy, viewModel::setReduceAnimations,
            )
            Spacer(Modifier.height(10.dp))
            ToggleRow(
                Icons.Filled.Memory, "Limit background activity",
                "Keeps at most 4 apps running in the background.",
                state.limitBackground, state.shizukuReady && !state.busy, viewModel::setLimitBackground,
            )
            state.message?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Switches show the live system value. Every change is saved in Fix History and can be undone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector, title: String, subtitle: String,
    checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit,
) {
    DocCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
        }
    }
}
