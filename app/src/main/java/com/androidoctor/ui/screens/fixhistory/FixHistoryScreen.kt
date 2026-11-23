package com.androidoctor.ui.screens.fixhistory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidoctor.data.local.entity.FixChangeEntity
import com.androidoctor.data.local.entity.FixSessionEntity
import com.androidoctor.ui.components.DocCard
import com.androidoctor.ui.components.DocScreen
import com.androidoctor.ui.components.EmptyNote
import com.androidoctor.ui.components.PrimaryButton
import com.androidoctor.ui.theme.DoctorTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FixHistoryScreen(
    onBack: () -> Unit,
    viewModel: FixHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DocScreen("Fix History", onBack) {
        if (state.sessions.isEmpty()) {
            EmptyNote("No changes yet. Anything the app changes will be listed here so you can undo it.")
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.sessions.forEach { session ->
                    SessionCard(
                        session = session,
                        expanded = state.expandedSessionId == session.id,
                        changes = state.changesBySession[session.id].orEmpty(),
                        isRollingBack = state.isRollingBack,
                        onToggle = { viewModel.toggleExpanded(session.id) },
                        onRollback = { viewModel.rollbackSession(session.id) },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private val timeFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

@Composable
private fun SessionCard(
    session: FixSessionEntity,
    expanded: Boolean,
    changes: List<FixChangeEntity>,
    isRollingBack: Boolean,
    onToggle: () -> Unit,
    onRollback: () -> Unit,
) {
    val activeCount = changes.count { !it.rolledBack }
    DocCard(Modifier.fillMaxWidth(), onClick = onToggle) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(session.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(timeFormat.format(Date(session.timestamp)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (expanded) "Hide" else "Show", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                if (changes.isEmpty()) {
                    Text("Loading...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    changes.forEach { ChangeRow(it) }
                    Spacer(Modifier.height(12.dp))
                    if (activeCount > 0) {
                        PrimaryButton(
                            if (isRollingBack) "Undoing..." else "Undo this session ($activeCount)",
                            onClick = onRollback,
                            enabled = !isRollingBack,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text("Everything was undone.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeRow(change: FixChangeEntity) {
    val c = DoctorTheme.colors
    val statusColor = when {
        change.rollbackFailed -> c.danger
        change.rolledBack -> c.success
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusText = when {
        change.rollbackFailed -> "failed: ${change.rollbackFailReason ?: "unknown"}"
        change.rolledBack -> "undone"
        else -> "active"
    }
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(change.target, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(statusText, fontSize = 12.sp, color = statusColor)
        }
        Text("${change.action}: ${change.original} → ${change.newValue}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
