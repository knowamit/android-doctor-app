package com.androidoctor.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.ads.InterstitialAdController
import com.androidoctor.data.shell.FixLog
import com.androidoctor.data.shell.FixLogState
import com.androidoctor.data.shell.ShellExecutor
import com.androidoctor.domain.model.DiagnosisResult
import com.androidoctor.domain.usecase.DiagnoseUseCase
import com.androidoctor.domain.usecase.OptimizeResult
import com.androidoctor.domain.usecase.OptimizeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val scanning: Boolean = true,
    val optimizing: Boolean = false,
    val score: Int? = null,
    val headline: String = "Checking your phone...",
    val shizukuReady: Boolean = false,
    val error: String? = null,
)

sealed interface HomeEvent {
    data class ShowResult(val result: OptimizeResult) : HomeEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val diagnose: DiagnoseUseCase,
    private val optimize: OptimizeUseCase,
    private val shell: ShellExecutor,
    private val ads: InterstitialAdController,
    fixLog: FixLog,
) : ViewModel() {

    val log: StateFlow<FixLogState> = fixLog.state

    private val channel = Channel<HomeEvent>(Channel.BUFFERED)
    /** One-shot events, buffered so a rotation during Optimize still delivers the result once. */
    val events: Flow<HomeEvent> = channel.receiveAsFlow()

    private var scanJob: Job? = null

    private val _state = MutableStateFlow(HomeUiState(shizukuReady = shell.isShizukuAvailable))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        scan()
    }

    fun refreshShizuku() {
        _state.value = _state.value.copy(shizukuReady = shell.isShizukuAvailable)
    }

    fun scan() {
        if (_state.value.optimizing) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _state.value = _state.value.copy(scanning = true, error = null, shizukuReady = shell.isShizukuAvailable)
            try {
                val r = diagnose()
                _state.value = _state.value.copy(
                    scanning = false,
                    score = r.verdict.overallScore,
                    headline = headlineFor(r),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    scanning = false,
                    headline = "Scan could not finish",
                    error = e.message,
                )
            }
        }
    }

    /** Runs the optimizations. The measured result arrives through [events]. */
    fun optimizeNow() {
        if (_state.value.optimizing) return
        // A running scan would share the log and the shell with the optimize run, so stop it first.
        scanJob?.cancel()
        viewModelScope.launch {
            _state.value = _state.value.copy(optimizing = true, scanning = false, error = null)
            try {
                val result = optimize()
                kotlinx.coroutines.delay(700)
                ads.onOptimizeFinished()
                _state.value = _state.value.copy(optimizing = false)
                channel.send(HomeEvent.ShowResult(result))
                scan()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(optimizing = false, error = e.message)
            }
        }
    }

    private fun headlineFor(r: DiagnosisResult): String {
        val v = r.verdict
        return when {
            v.overallScore >= 80 -> "Your phone is in good shape!"
            v.overallScore >= 60 -> v.topIssues.firstOrNull() ?: "Your phone could run better"
            else -> v.topIssues.firstOrNull() ?: "Your phone needs attention"
        }
    }
}
