package com.aggreyshield.app.presentation.incidents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.Loadable
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.map
import com.aggreyshield.app.core.common.toLoadable
import com.aggreyshield.app.domain.model.Incident
import com.aggreyshield.app.domain.model.IncidentStatus
import com.aggreyshield.app.domain.repository.IncidentRepository
import com.aggreyshield.app.navigation.Routes
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

data class IncidentListUiState(
    val incidents: Loadable<List<Incident>> = Loadable.Loading,
    val selectedStatus: IncidentStatus? = null,
)

@HiltViewModel
class IncidentListViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncidentListUiState())
    val uiState: StateFlow<IncidentListUiState> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onStatusSelected(status: IncidentStatus?) {
        _uiState.update { it.copy(selectedStatus = status) }
        load()
    }

    fun onIncidentClicked(id: String) {
        viewModelScope.launch { _events.send(OneShotEvent.Navigate(Routes.incidentDetail(id))) }
    }

    fun onReportClicked() {
        viewModelScope.launch { _events.send(OneShotEvent.Navigate(Routes.INCIDENT_REPORT)) }
    }

    private fun load() {
        _uiState.update { it.copy(incidents = Loadable.Loading) }
        viewModelScope.launch {
            val status = _uiState.value.selectedStatus
            val result = incidentRepository.list(status = status, limit = 50, offset = 0).map { it.items }
            _uiState.update { it.copy(incidents = result.toLoadable()) }
        }
    }
}
