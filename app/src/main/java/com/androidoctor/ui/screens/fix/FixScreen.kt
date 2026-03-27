package com.androidoctor.ui.screens.fix

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androidoctor.domain.model.Impact
import com.androidoctor.domain.usecase.FixLevel
import com.androidoctor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixScreen(
    onBack: () -> Unit,
    viewModel: FixViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FIX", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Zinc950),
            )
        },
        containerColor = Zinc950,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            if (state.isLoading) {
                Spacer(modifier = Modifier.height(100.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Copper)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(state.step, color = Zinc400, fontSize = 13.sp)
                    }
                }
                return@Scaffold
            }

            // Results
            state.result?.let { result ->
                Spacer(modifier = Modifier.height(16.dp))
                Text("FIX COMPLETE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ScoreGreen, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Zinc800, RoundedCornerShape(4.dp))
                        .padding(16.dp),
                ) {
                    ResultRow("Packages disabled", result.packagesDisabled.toString())
                    ResultRow("Settings optimized", result.settingsChanged.toString())
                    ResultRow("Battery optimizations", result.batteryOptimized.toString())
                    if (result.cacheFreedBytes > 0) {
                        val mb = result.cacheFreedBytes / (1024 * 1024)
                        ResultRow("Cache freed", "${mb}MB")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("All changes are reversible.", fontSize = 12.sp, color = Zinc400)
                Spacer(modifier = Modifier.height(24.dp))
            }

            state.rollbackResult?.let { count ->
                Spacer(modifier = Modifier.height(16.dp))
                Text("ROLLBACK COMPLETE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ScoreGreen, letterSpacing = 2.sp)
                Text("$count changes restored.", fontSize = 13.sp, color = Zinc300, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bloatware list
            if (state.bloatware.isNotEmpty()) {
                Text("BLOATWARE FOUND", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Zinc400, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("${state.bloatware.size} removable packages", fontSize = 13.sp, color = Zinc300)
                Spacer(modifier = Modifier.height(12.dp))

                for (entry in state.bloatware.take(10)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val impactColor = when (entry.impact) {
                            Impact.HIGH -> ScoreRed
                            Impact.MEDIUM -> ScoreYellow
                            Impact.LOW -> Zinc700
                        }
                        Text(
                            text = entry.impact.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = impactColor,
                            modifier = Modifier.width(40.dp),
                        )
                        Column {
                            Text(entry.name, fontSize = 13.sp, color = Zinc100)
                            Text(entry.packageName, fontSize = 10.sp, color = Zinc700)
                        }
                    }
                }
                if (state.bloatware.size > 10) {
                    Text("+ ${state.bloatware.size - 10} more", fontSize = 11.sp, color = Zinc700, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons — solid/outline, no gradient
            Button(
                onClick = { viewModel.runFix(FixLevel.SAFE) },
                colors = ButtonDefaults.buttonColors(containerColor = Copper),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Fix (Safe)", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { viewModel.runFix(FixLevel.MODERATE) },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Fix (Moderate)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { viewModel.runFix(FixLevel.AGGRESSIVE) },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Fix (Aggressive)")
            }

            if (state.hasSnapshot) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { viewModel.rollback() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ScoreYellow),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Rollback All Changes")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = Zinc400)
        Text(value, fontSize = 13.sp, color = Zinc100, fontWeight = FontWeight.SemiBold)
    }
}
