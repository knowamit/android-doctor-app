package com.androidoctor.ui.screens.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.data.shell.ShellExecutor
import com.androidoctor.data.storage.StorageScan
import com.androidoctor.data.storage.StorageScanner
import com.androidoctor.domain.repository.FixRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageUiState(
    val loading: Boolean = true,
    val scan: StorageScan? = null,
    val cleaning: Boolean = false,
    val freedBytes: Long? = null, // measured df delta from the last clean
    val shizukuReady: Boolean = false,
)

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val scanner: StorageScanner,
    private val fix: FixRepository,
    private val shell: ShellExecutor,
) : ViewModel() {
    private val _state = MutableStateFlow(StorageUiState())
    val state: StateFlow<StorageUiState> = _state.asStateFlow()

    private var scanJob: Job? = null

    // No init refresh: the screen refreshes on ON_RESUME, which also fires on first entry.
    fun refresh() {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, shizukuReady = shell.isShizukuAvailable)
            val scan = scanner.scan()
            _state.value = _state.value.copy(loading = false, scan = scan)
        }
    }

    fun cleanNow() {
        if (_state.value.cleaning || !shell.isShizukuAvailable) return
        viewModelScope.launch {
            _state.value = _state.value.copy(cleaning = true)
            fix.beginSession("Clean Storage")
            val freed = try { fix.trimCaches() } finally { fix.endSession() }
            _state.value = _state.value.copy(cleaning = false, freedBytes = freed)
            scanJob?.join()
            refresh()
        }
    }
}
