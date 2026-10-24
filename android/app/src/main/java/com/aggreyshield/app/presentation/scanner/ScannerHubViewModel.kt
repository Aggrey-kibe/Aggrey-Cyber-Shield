package com.aggreyshield.app.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.Loadable
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.map
import com.aggreyshield.app.core.common.toLoadable
import com.aggreyshield.app.domain.model.ScanHistoryItem
import com.aggreyshield.app.domain.repository.ScanRepository
import com.aggreyshield.app.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannerHubViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<Loadable<List<ScanHistoryItem>>>(Loadable.Loading)
    val uiState: StateFlow<Loadable<List<ScanHistoryItem>>> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        _uiState.value = Loadable.Loading
        viewModelScope.launch {
            _uiState.value = scanRepository.history(limit = 20).map { it.items }.toLoadable()
        }
    }

    fun onUrlScannerClicked() = navigate(Routes.URL_SCANNER)
    fun onMessageAnalyzerClicked() = navigate(Routes.MESSAGE_ANALYZER)

    private fun navigate(route: String) {
        viewModelScope.launch { _events.send(OneShotEvent.Navigate(route)) }
    }
}
