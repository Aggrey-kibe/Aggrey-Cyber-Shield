package com.aggreyshield.app.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.InputValidator
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.UrlCheck
import com.aggreyshield.app.domain.model.ScanResult
import com.aggreyshield.app.domain.repository.ScanRepository
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

data class UrlScannerUiState(
    val url: String = "",
    val urlError: String? = null,
    val isScanning: Boolean = false,
    val result: ScanResult? = null,
)

@HiltViewModel
class UrlScannerViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UrlScannerUiState())
    val uiState: StateFlow<UrlScannerUiState> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    fun onUrlChanged(value: String) = _uiState.update { it.copy(url = value, urlError = null, result = null) }

    fun onScan() {
        val check = InputValidator.checkUrl(_uiState.value.url)
        val validUrl = when (check) {
            is UrlCheck.Invalid -> {
                _uiState.update { it.copy(urlError = check.reason) }
                return
            }
            is UrlCheck.Valid -> check.url
        }

        _uiState.update { it.copy(isScanning = true, result = null) }
        viewModelScope.launch {
            val result = scanRepository.scanUrl(validUrl)
            _uiState.update { it.copy(isScanning = false) }
            when (result) {
                is ApiResult.Success -> _uiState.update { it.copy(result = result.data) }
                is ApiResult.Failure -> _events.send(OneShotEvent.ShowMessage(result.error.userMessage))
            }
        }
    }
}
