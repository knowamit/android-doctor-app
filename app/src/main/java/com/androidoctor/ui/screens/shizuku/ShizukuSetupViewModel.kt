package com.androidoctor.ui.screens.shizuku

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.data.analytics.AnalyticsLogger
import com.androidoctor.data.shell.ShellExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShizukuSetupState(
    val shizukuInstalled: Boolean = false,
    val shizukuRunning: Boolean = false,
    val checking: Boolean = true,
)

@HiltViewModel
class ShizukuSetupViewModel @Inject constructor(
    private val shell: ShellExecutor,
    private val analytics: AnalyticsLogger,
) : ViewModel() {

    private val _state = MutableStateFlow(ShizukuSetupState())
    val state: StateFlow<ShizukuSetupState> = _state.asStateFlow()

    init {
        poll()
    }

    fun refresh() = poll()

    private fun poll() {
        viewModelScope.launch {
            val wasRunning = _state.value.shizukuRunning
            _state.value = _state.value.copy(checking = true)
            val running = shell.isShizukuAvailable
            _state.value = ShizukuSetupState(
                shizukuInstalled = running || packageExists("moe.shizuku.privileged.api"),
                shizukuRunning = running,
                checking = false,
            )
            if (running && !wasRunning) analytics.shizukuGranted()
        }
    }

    private suspend fun packageExists(pkg: String): Boolean {
        val r = shell.exec("pm list packages $pkg")
        return r.output.contains(pkg)
    }
}
