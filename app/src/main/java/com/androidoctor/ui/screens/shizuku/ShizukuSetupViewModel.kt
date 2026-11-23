package com.androidoctor.ui.screens.shizuku

import android.content.Context
import androidx.lifecycle.ViewModel
import com.androidoctor.data.analytics.AnalyticsLogger
import com.androidoctor.data.shell.ShellExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

data class ShizukuSetupState(
    val installed: Boolean = false,
    val running: Boolean = false,   // Shizuku service is up
    val granted: Boolean = false,   // this app may use it
    val denied: Boolean = false,    // user chose "deny" permanently
)

@HiltViewModel
class ShizukuSetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shell: ShellExecutor,
    private val analytics: AnalyticsLogger,
) : ViewModel() {

    private val _state = MutableStateFlow(ShizukuSetupState())
    val state: StateFlow<ShizukuSetupState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        val wasGranted = _state.value.granted
        val running = shell.isShizukuInstalled
        val granted = shell.isShizukuAvailable
        val installed = running || runCatching {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        }.isSuccess
        _state.value = _state.value.copy(installed = installed, running = running, granted = granted)
        if (granted && !wasGranted) analytics.shizukuGranted()
    }

    fun requestPermission() {
        shell.requestShizukuPermission { ok ->
            refresh()
            if (!ok && !_state.value.granted) _state.value = _state.value.copy(denied = true)
        }
    }
}
