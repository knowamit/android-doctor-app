package com.androidoctor.ui.screens.home

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.ads.ScanCompleteAdController
import com.androidoctor.data.local.ScanCountStore
import com.androidoctor.domain.model.QuickScanResult
import com.androidoctor.domain.usecase.QuickScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow

sealed class QuickScanUiState {
    data object Idle : QuickScanUiState()
    data class Scanning(val durationMs: Long) : QuickScanUiState()
    data class Done(val result: QuickScanResult) : QuickScanUiState()
}

// First-ever scan runs long (~55s) to feel thorough. Each later scan decays
// toward a floor, as if the app "learned" the device and got faster.
private const val FIRST_SCAN_DURATION_MS = 55_000L
private const val SCAN_DECAY_FACTOR = 0.72
private const val MIN_SCAN_DURATION_MS = 1_400L

private fun durationForScanIndex(index: Int): Long {
    val decayed = FIRST_SCAN_DURATION_MS * SCAN_DECAY_FACTOR.pow(index)
    return decayed.toLong().coerceAtLeast(MIN_SCAN_DURATION_MS)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val quickScanUseCase: QuickScanUseCase,
    private val scanCountStore: ScanCountStore,
    private val adController: ScanCompleteAdController,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuickScanUiState>(QuickScanUiState.Idle)
    val uiState: StateFlow<QuickScanUiState> = _uiState.asStateFlow()

    init {
        // Preload during the long first scan so it's ready by the time
        // results are in — no wait before the full-screen ad shows.
        adController.preload(appContext)
    }

    fun showCompleteAd(activity: Activity, onDismissed: () -> Unit) {
        adController.show(activity, onDismissed)
    }

    fun startScan() {
        if (_uiState.value is QuickScanUiState.Scanning) return
        viewModelScope.launch {
            val scanIndex = scanCountStore.getAndIncrement()
            val duration = durationForScanIndex(scanIndex)
            _uiState.value = QuickScanUiState.Scanning(duration)
            val minDuration = async { delay(duration) }
            val result = quickScanUseCase()
            minDuration.await()
            _uiState.value = QuickScanUiState.Done(result)
        }
    }
}
