package com.androidoctor.ui.screens.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.data.shell.ShellExecutor
import com.androidoctor.domain.repository.FixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PerformanceUiState(
    val shizukuReady: Boolean = false,
    val reduceAnimations: Boolean = false,
    val limitBackground: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class PerformanceViewModel @Inject constructor(
    private val shell: ShellExecutor,
    private val fix: FixRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PerformanceUiState())
    val state: StateFlow<PerformanceUiState> = _state.asStateFlow()

    init { refresh() }

    /** Reads the real current system values, so the switches never lie. */
    fun refresh() {
        viewModelScope.launch {
            val ready = shell.isShizukuAvailable
            val anim = shell.exec("settings get global window_animation_scale").output.toFloatOrNull()
            val limit = shell.exec("settings get global background_process_limit").output.toIntOrNull()
            _state.value = _state.value.copy(
                shizukuReady = ready,
                reduceAnimations = anim != null && anim < 1f,
                limitBackground = limit != null && limit >= 0,
            )
        }
    }

    fun setReduceAnimations(on: Boolean) = apply("Animations") {
        fix.setAnimationScale(if (on) 0.5f else 1.0f)
    }

    fun setLimitBackground(on: Boolean) = apply("Background limit") {
        fix.setBackgroundProcessLimit(if (on) 4 else -1)
    }

    private fun apply(label: String, block: suspend () -> Boolean) {
        if (_state.value.busy || !shell.isShizukuAvailable) return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = null)
            fix.beginSession(label)
            val ok = try { block() } finally { fix.endSession() }
            _state.value = _state.value.copy(busy = false, message = if (ok) null else "Could not change $label")
            refresh()
        }
    }
}
