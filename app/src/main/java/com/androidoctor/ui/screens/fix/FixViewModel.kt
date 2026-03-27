package com.androidoctor.ui.screens.fix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.domain.model.BloatwareEntry
import com.androidoctor.domain.repository.DiagnosisRepository
import com.androidoctor.domain.usecase.FixLevel
import com.androidoctor.domain.usecase.FixResult
import com.androidoctor.domain.usecase.FixUseCase
import com.androidoctor.domain.usecase.RollbackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val rollbackResult: Int? = null,
    val hasSnapshot: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FixViewModel @Inject constructor(
    private val fixUseCase: FixUseCase,
    private val rollbackUseCase: RollbackUseCase,
    private val diagnosisRepository: DiagnosisRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FixUiState())
    val state: StateFlow<FixUiState> = _state.asStateFlow()

    init {
        _state.value = _state.value.copy(hasSnapshot = rollbackUseCase.hasSnapshot())
        loadBloatware()
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

    fun runFix(level: FixLevel = FixLevel.SAFE) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, step = "Applying fixes...")
            try {
                val result = fixUseCase(_state.value.bloatware, level)
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
                val count = rollbackUseCase()
                _state.value = _state.value.copy(
                    isLoading = false,
                    rollbackResult = count,
                    hasSnapshot = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
