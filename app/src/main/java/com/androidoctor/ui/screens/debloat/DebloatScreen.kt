package com.androidoctor.ui.screens.debloat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.androidoctor.domain.model.BloatwareCatalogEntry
import com.androidoctor.ui.components.DocCard
import com.androidoctor.ui.components.DocScreen
import com.androidoctor.ui.components.EmptyNote
import com.androidoctor.ui.components.PrimaryButton
import com.androidoctor.ui.components.StatusPill
import com.androidoctor.ui.components.openAppSettings
import com.androidoctor.ui.theme.DoctorTheme

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DebloatScreen(onBack: () -> Unit, onNeedShizuku: () -> Unit, viewModel: DebloatViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    DocScreen("Debloat Apps", onBack) {
        Column(Modifier.fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = state.tab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                DebloatTab.entries.forEach { t ->
                    Tab(selected = state.tab == t, onClick = { viewModel.selectTab(t) }, text = { Text(t.name, maxLines = 1, fontSize = 13.sp) })
                }
            }
            Box(Modifier.weight(1f)) {
                when {
                    state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.visible.isEmpty() -> EmptyNote(
                        if (state.tab == DebloatTab.Disabled) "Nothing disabled yet." else "No known bloat found on this phone.",
                    )
                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        items(state.visible, key = { it.packageName }) { e ->
                            Row(
                                entry = e,
                                checked = e.packageName in state.selected,
                                showCheckbox = state.shizukuReady,
                                onToggle = { viewModel.toggle(e.packageName) },
                                onOpenSettings = { openAppSettings(ctx, e.packageName) },
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
            state.message?.let {
                Text(it, Modifier.padding(horizontal = 20.dp), color = DoctorTheme.colors.success, fontSize = 13.sp)
            }
            val n = state.selected.size
            val verb = if (state.tab == DebloatTab.Disabled) "Enable" else "Disable"
            Column(Modifier.navigationBarsPadding().padding(20.dp)) {
                when {
                    state.working -> PrimaryButton("Working...", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
                    !state.shizukuReady -> androidx.compose.material3.TextButton(onClick = onNeedShizuku, modifier = Modifier.fillMaxWidth()) {
                        Text("Disable many at once with Shizuku")
                    }
                    else -> PrimaryButton("$verb Selected ($n)", onClick = viewModel::applySelected, enabled = n > 0, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun Row(
    entry: BloatwareCatalogEntry,
    checked: Boolean,
    showCheckbox: Boolean,
    onToggle: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    DocCard(Modifier.fillMaxWidth(), onClick = if (showCheckbox) onToggle else onOpenSettings) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                val bits = buildList {
                    if (entry.ramMb > 0) add("${entry.ramMb} MB RAM")
                    if (entry.notes.isNotBlank()) add(entry.notes)
                }.joinToString(" - ")
                if (bits.isNotBlank()) Text(bits, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Spacer(Modifier.height(6.dp))
                if (entry.safe) StatusPill("Safe", DoctorTheme.colors.success) else StatusPill("Review", DoctorTheme.colors.warning)
            }
            Spacer(Modifier.width(8.dp))
            if (showCheckbox) {
                Checkbox(checked = checked, onCheckedChange = { onToggle() })
            } else {
                Text("Open app settings", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
