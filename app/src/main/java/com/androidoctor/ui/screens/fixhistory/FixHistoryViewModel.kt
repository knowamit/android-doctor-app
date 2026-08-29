package com.androidoctor.ui.screens.fixhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.data.local.dao.FixChangeDao
import com.androidoctor.data.local.dao.FixSessionDao
import com.androidoctor.data.local.entity.FixChangeEntity
import com.androidoctor.data.local.entity.FixSessionEntity
import com.androidoctor.domain.usecase.RollbackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FixHistoryUiState(
    val sessions: List<FixSessionEntity> = emptyList(),
    val expandedSessionId: Long? = null,
    val changesBySession: Map<Long, List<FixChangeEntity>> = emptyMap(),
    val isRollingBack: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FixHistoryViewModel @Inject constructor(
    private val sessionDao: FixSessionDao,
    private val changeDao: FixChangeDao,
    private val rollbackUseCase: RollbackUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(FixHistoryUiState())
    val state: StateFlow<FixHistoryUiState> = _state.asStateFlow()

    init {
        sessionDao.getAll()
            .onEach { sessions -> _state.value = _state.value.copy(sessions = sessions) }
            .launchIn(viewModelScope)
    }

    fun toggleExpanded(sessionId: Long) {
        val current = _state.value.expandedSessionId
        if (current == sessionId) {
            _state.value = _state.value.copy(expandedSessionId = null)
            return
        }
        _state.value = _state.value.copy(expandedSessionId = sessionId)
        viewModelScope.launch {
            changeDao.getForSession(sessionId)
                .onEach { changes ->
                    _state.value = _state.value.copy(
                        changesBySession = _state.value.changesBySession + (sessionId to changes),
                    )
                }
                .launchIn(viewModelScope)
        }
    }

    fun rollbackSession(sessionId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRollingBack = true)
            try {
                rollbackUseCase(sessionId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            } finally {
                _state.value = _state.value.copy(isRollingBack = false)
            }
        }
    }
}
