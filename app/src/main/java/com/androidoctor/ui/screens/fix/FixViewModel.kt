package com.androidoctor.ui.screens.fix

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.data.shell.FixLog
import com.androidoctor.data.shell.FixLogState
import com.androidoctor.data.shell.ShellExecutor
import com.androidoctor.data.standard.StandardFixResult
import com.androidoctor.data.standard.StandardFixer
import com.androidoctor.domain.model.BloatwareEntry
import com.androidoctor.domain.repository.DiagnosisRepository
import com.androidoctor.domain.repository.PanicBoostResult
import com.androidoctor.domain.usecase.CpuBoostUseCase
import com.androidoctor.domain.usecase.FixLevel
import com.androidoctor.domain.usecase.FixResult
import com.androidoctor.domain.usecase.FixUseCase
import com.androidoctor.domain.usecase.PanicBoostUseCase
import com.androidoctor.domain.usecase.RollbackUseCase
import com.androidoctor.worker.RestoreAnimationsWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FixUiState(
    val isLoading: Boolean = false,
    val step: String = "",
    val bloatware: List<BloatwareEntry> = emptyList(),
    val result: FixResult? = null,
    val rollbackMessage: String? = null,
    val hasSnapshot: Boolean = false,
    val error: String? = null,
    val shizukuAvailable: Boolean = false,
    val shizukuRunning: Boolean = false,
    val panicResult: PanicBoostResult? = null,
    val cpuGovernor: String? = null,
    val cpuBoostActive: Boolean = false,
    val cpuBoostSupported: Boolean = true,
    val cpuBoostMessage: String? = null,
    val standardResult: StandardFixResult? = null,
)

@HiltViewModel
class FixViewModel @Inject constructor(
    private val fixUseCase: FixUseCase,
    private val rollbackUseCase: RollbackUseCase,
    private val panicBoost: PanicBoostUseCase,
    private val cpuBoost: CpuBoostUseCase,
    private val diagnosisRepository: DiagnosisRepository,
    private val shell: ShellExecutor,
    private val standardFixer: StandardFixer,
    @ApplicationContext private val appContext: Context,
    fixLog: FixLog,
) : ViewModel() {

    val log: StateFlow<FixLogState> = fixLog.state

    private val _state = MutableStateFlow(FixUiState())
    val state: StateFlow<FixUiState> = _state.asStateFlow()

    init {
        refreshShizuku()
        viewModelScope.launch {
            _state.value = _state.value.copy(hasSnapshot = rollbackUseCase.hasSnapshot())
        }
        loadBloatware()
    }

    fun refreshShizuku() {
        _state.value = _state.value.copy(
            shizukuAvailable = shell.isShizukuAvailable,
            shizukuRunning = shell.isShizukuInstalled,
        )
        loadGovernor()
    }

    /** Reads the current CPU governor. If it cannot be read, the CPU boost control is hidden. */
    private fun loadGovernor() {
        viewModelScope.launch {
            val g = try { cpuBoost.current() } catch (e: Exception) { null }
            _state.value = _state.value.copy(cpuGovernor = g, cpuBoostActive = g == "performance")
        }
    }

    fun requestShizukuAccess() {
        shell.requestShizukuPermission { refreshShizuku() }
    }

    private fun loadBloatware() {
        viewModelScope.launch {
            try {
                val diag = diagnosisRepository.diagnoseBloatware()
                _state.value = _state.value.copy(bloatware = diag.removable)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun runStandardFix() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, standardResult = null)
            try {
                val r = standardFixer.run()
                kotlinx.coroutines.delay(700)
                _state.value = _state.value.copy(isLoading = false, standardResult = r)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun runFix(level: FixLevel = FixLevel.SAFE) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, step = "Applying fixes...")
            try {
                val result = fixUseCase(_state.value.bloatware, level)
                kotlinx.coroutines.delay(700)
                _state.value = _state.value.copy(
                    isLoading = false,
                    result = result,
                    hasSnapshot = rollbackUseCase.hasSnapshot(),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun rollback() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, step = "Rolling back...")
            try {
                val out = rollbackUseCase.latest()
                val msg = when {
                    out.restored == 0 && out.kept == 0 -> "Nothing to undo."
                    out.kept > 0 -> "Restored ${out.restored} settings. Some changes were kept because a newer fix changed the same setting. See Fix History."
                    else -> "Restored ${out.restored} settings."
                }
                _state.value = _state.value.copy(
                    isLoading = false,
                    rollbackMessage = msg,
                    hasSnapshot = rollbackUseCase.hasSnapshot(),
                    cpuBoostActive = false,
                )
                loadGovernor()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun runPanicBoost() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, step = "Panic boost running...")
            try {
                val result = panicBoost()
                // Animations were turned off: bring them back after 5 minutes.
                if (result.animationsZeroed && result.sessionId != 0L) {
                    RestoreAnimationsWorker.schedule(appContext, result.sessionId)
                }
                kotlinx.coroutines.delay(700)
                _state.value = _state.value.copy(
                    isLoading = false,
                    panicResult = result,
                    hasSnapshot = rollbackUseCase.hasSnapshot(),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleCpuBoost() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, step = "Changing CPU governor...")
            try {
                if (_state.value.cpuBoostActive) {
                    cpuBoost.disable()
                    _state.value = _state.value.copy(isLoading = false, cpuBoostMessage = null)
                } else {
                    val cores = cpuBoost.enable()
                    if (cores == 0) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            cpuBoostSupported = false,
                            cpuBoostMessage = "CPU boost is not supported on this phone.",
                        )
                    } else {
                        _state.value = _state.value.copy(isLoading = false, cpuBoostMessage = null)
                    }
                }
                // Only report what the phone says now, not what we asked for.
                val g = cpuBoost.current()
                _state.value = _state.value.copy(
                    cpuGovernor = g,
                    cpuBoostActive = g == "performance",
                    hasSnapshot = rollbackUseCase.hasSnapshot(),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun consumePanicResult() {
        _state.value = _state.value.copy(panicResult = null)
    }
}
