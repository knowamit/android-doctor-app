package com.androidoctor.ui.screens.fixhistory

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidoctor.data.local.entity.FixChangeEntity
import com.androidoctor.data.local.entity.FixSessionEntity
import com.androidoctor.ui.theme.*
import com.androidoctor.ui.components.GlassShape
import com.androidoctor.ui.components.glassSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixHistoryScreen(
    onBack: () -> Unit,
    viewModel: FixHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FIX HISTORY", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
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
            Spacer(modifier = Modifier.height(8.dp))
            if (state.sessions.isEmpty()) {
                Spacer(modifier = Modifier.height(40.dp))
                Text("No fixes applied yet.", fontSize = 14.sp, color = Zinc400)
            } else {
                for (session in state.sessions) {
                    SessionCard(
                        session = session,
                        expanded = state.expandedSessionId == session.id,
                        changes = state.changesBySession[session.id].orEmpty(),
                        isRollingBack = state.isRollingBack,
                        onToggle = { viewModel.toggleExpanded(session.id) },
                        onRollback = { viewModel.rollbackSession(session.id) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(borderColor = Zinc800)
            .clickable(onClick = onToggle)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(session.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Zinc100)
                Text(timeFormat.format(Date(session.timestamp)), fontSize = 11.sp, color = Zinc400)
            }
            Text(
                if (expanded) "▲" else "▼",
                fontSize = 12.sp,
                color = Zinc400,
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            if (changes.isEmpty()) {
                Text("Loading...", fontSize = 12.sp, color = Zinc400)
            } else {
                for (change in changes) {
                    ChangeRow(change)
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (activeCount > 0) {
                    Button(
                        onClick = onRollback,
                        enabled = !isRollingBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Copper),
                        shape = GlassShape,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (isRollingBack) "Rolling back..." else "Rollback This Session ($activeCount)")
                    }
                } else {
                    Text("All changes rolled back.", fontSize = 12.sp, color = Zinc400)
                }
            }
        }
    }
}

@Composable
private fun ChangeRow(change: FixChangeEntity) {
    val statusColor = when {
        change.rollbackFailed -> ScoreRed
        change.rolledBack -> ScoreGreen
        else -> Zinc400
    }
    val statusText = when {
        change.rollbackFailed -> "failed: ${change.rollbackFailReason ?: "unknown"}"
        change.rolledBack -> "rolled back"
        else -> "active"
    }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(change.target, fontSize = 12.sp, color = Zinc300)
            Text(statusText, fontSize = 11.sp, color = statusColor)
        }
        Text("${change.action}: ${change.original} → ${change.newValue}", fontSize = 11.sp, color = Zinc700)
    }
}
