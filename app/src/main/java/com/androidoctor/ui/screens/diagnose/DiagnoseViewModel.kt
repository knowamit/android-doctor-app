package com.androidoctor.ui.screens.diagnose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.domain.model.DiagnosisResult
import com.androidoctor.domain.usecase.DiagnoseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnoseUiState(
    val isLoading: Boolean = false,
    val currentStep: String = "",
    val result: DiagnosisResult? = null,
    val error: String? = null,
)

@HiltViewModel
class DiagnoseViewModel @Inject constructor(
    private val diagnoseUseCase: DiagnoseUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DiagnoseUiState())
    val state: StateFlow<DiagnoseUiState> = _state.asStateFlow()

    fun runDiagnosis() {
        viewModelScope.launch {
            _state.value = DiagnoseUiState(isLoading = true, currentStep = "Running diagnostics...")
            try {
                val result = diagnoseUseCase()
                _state.value = DiagnoseUiState(result = result)
            } catch (e: Exception) {
                _state.value = DiagnoseUiState(error = e.message ?: "Diagnosis failed")
            }
        }
    }
}
