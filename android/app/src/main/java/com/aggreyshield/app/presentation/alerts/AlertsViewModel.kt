package com.aggreyshield.app.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.Loadable
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.map
import com.aggreyshield.app.core.common.toLoadable
import com.aggreyshield.app.domain.model.AlertStatus
import com.aggreyshield.app.domain.model.SecurityAlert
import com.aggreyshield.app.domain.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val alerts: Loadable<List<SecurityAlert>> = Loadable.Loading,
    val selectedStatus: AlertStatus? = null,
    val acknowledgingId: String? = null,
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onStatusSelected(status: AlertStatus?) {
        _uiState.update { it.copy(selectedStatus = status) }
        load()
    }

    fun onAcknowledge(id: String) {
        _uiState.update { it.copy(acknowledgingId = id) }
        viewModelScope.launch {
            val result = alertRepository.acknowledge(id)
            _uiState.update { it.copy(acknowledgingId = null) }
            when (result) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _events.send(OneShotEvent.ShowMessage(result.error.userMessage))
            }
        }
    }

    private fun load() {
        _uiState.update { it.copy(alerts = Loadable.Loading) }
        viewModelScope.launch {
            val status = _uiState.value.selectedStatus
            val result = alertRepository.list(status = status, limit = 50, offset = 0).map { it.items }
            _uiState.update { it.copy(alerts = result.toLoadable()) }
        }
    }
}
