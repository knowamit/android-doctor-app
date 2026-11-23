package com.androidoctor.ui.screens.debloat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.data.parser.BloatwareCatalogLoader
import com.androidoctor.data.shell.ShellExecutor
import com.androidoctor.domain.model.BloatwareCatalogEntry
import com.androidoctor.domain.repository.FixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DebloatTab { Recommended, All, Disabled }

data class DebloatUiState(
    val loading: Boolean = true,
    val enabled: List<BloatwareCatalogEntry> = emptyList(),
    val disabled: List<BloatwareCatalogEntry> = emptyList(),
    val tab: DebloatTab = DebloatTab.Recommended,
    val selected: Set<String> = emptySet(),
    val working: Boolean = false,
    val message: String? = null,
    val shizukuReady: Boolean = false,
) {
    val visible: List<BloatwareCatalogEntry>
        get() = when (tab) {
            DebloatTab.Recommended -> enabled.filter { it.safe }
            DebloatTab.All -> enabled
            DebloatTab.Disabled -> disabled
        }
}

@HiltViewModel
class DebloatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loader: BloatwareCatalogLoader,
    private val fix: FixRepository,
    private val shell: ShellExecutor,
) : ViewModel() {
    private val _state = MutableStateFlow(DebloatUiState())
    val state: StateFlow<DebloatUiState> = _state.asStateFlow()

    init { load() }

    private fun isEnabled(pkg: String): Boolean =
        runCatching { context.packageManager.getApplicationInfo(pkg, 0).enabled }.getOrDefault(false)

    private fun load() {
        viewModelScope.launch {
            val catalog = loader.load()
            val ready = shell.isShizukuAvailable
            val on: List<BloatwareCatalogEntry>
            val off: List<BloatwareCatalogEntry>
            if (ready) {
                // Shell sees every package, including disabled ones and apps without a launcher icon.
                val all = packageSet("pm list packages")
                val disabledPkgs = packageSet("pm list packages -d")
                val present = catalog.entries.filter { it.packageName in all }
                on = present.filter { it.packageName !in disabledPkgs }
                off = present.filter { it.packageName in disabledPkgs }
            } else {
                val (e, d) = catalog.installedEntries.partition { isEnabled(it.packageName) }
                on = e
                off = d
            }
            _state.value = _state.value.copy(
                loading = false,
                enabled = on.sortedByDescending { it.ramMb },
                disabled = off,
                shizukuReady = ready,
            )
        }
    }

    private suspend fun packageSet(cmd: String): Set<String> =
        shell.exec(cmd).stdout.lineSequence()
            .map { it.trim().removePrefix("package:") }
            .filter { it.isNotEmpty() }
            .toSet()

    fun selectTab(t: DebloatTab) { _state.value = _state.value.copy(tab = t, selected = emptySet()) }

    fun toggle(pkg: String) {
        val s = _state.value.selected
        _state.value = _state.value.copy(selected = if (pkg in s) s - pkg else s + pkg)
    }

    fun dismissMessage() { _state.value = _state.value.copy(message = null) }

    /** Disables selected packages (or re-enables them on the Disabled tab). */
    fun applySelected() {
        val s = _state.value
        if (s.working || s.selected.isEmpty() || !shell.isShizukuAvailable) return
        val enabling = s.tab == DebloatTab.Disabled
        viewModelScope.launch {
            _state.value = s.copy(working = true)
            fix.beginSession(if (enabling) "Enable apps" else "Disable apps")
            var ok = 0
            try {
                for (pkg in s.selected) {
                    if (if (enabling) fix.enablePackage(pkg) else fix.disablePackage(pkg)) ok++
                }
            } finally { fix.endSession() }
            val verb = if (enabling) "Enabled" else "Disabled"
            _state.value = _state.value.copy(
                working = false,
                selected = emptySet(),
                message = "$verb $ok of ${s.selected.size} apps",
            )
            load()
        }
    }
}
