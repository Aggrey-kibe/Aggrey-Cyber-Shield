package com.aggreyshield.app.presentation.threatintel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.Loadable
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.map
import com.aggreyshield.app.core.common.toLoadable
import com.aggreyshield.app.domain.model.IndicatorType
import com.aggreyshield.app.domain.model.ThreatIndicator
import com.aggreyshield.app.domain.repository.ThreatIntelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThreatIntelUiState(
    val indicators: Loadable<List<ThreatIndicator>> = Loadable.Loading,
    val query: String = "",
    val selectedType: IndicatorType? = null,
)

@HiltViewModel
class ThreatIntelViewModel @Inject constructor(
    private val threatIntelRepository: ThreatIntelRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThreatIntelUiState())
    val uiState: StateFlow<ThreatIntelUiState> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onRetry() = load()
    fun onQueryChanged(value: String) = _uiState.update { it.copy(query = value) }
    fun onSearch() = load()

    fun onTypeSelected(type: IndicatorType?) {
        _uiState.update { it.copy(selectedType = type) }
        load()
    }

    private fun load() {
        _uiState.update { it.copy(indicators = Loadable.Loading) }
        viewModelScope.launch {
            val state = _uiState.value
            val result = threatIntelRepository
                .indicators(type = state.selectedType, query = state.query, limit = 50, offset = 0)
                .map { it.items }
            _uiState.update { it.copy(indicators = result.toLoadable()) }
        }
    }
}
