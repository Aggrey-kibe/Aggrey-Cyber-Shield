package com.aggreyshield.app.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.InputValidator
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.domain.model.MessageAnalysis
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

data class MessageAnalyzerUiState(
    val content: String = "",
    val contentError: String? = null,
    val isAnalyzing: Boolean = false,
    val result: MessageAnalysis? = null,
)

@HiltViewModel
class MessageAnalyzerViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageAnalyzerUiState())
    val uiState: StateFlow<MessageAnalyzerUiState> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    fun onContentChanged(value: String) = _uiState.update { it.copy(content = value, contentError = null, result = null) }

    fun onAnalyze() {
        val contentError = InputValidator.validateMessageContent(_uiState.value.content)
        if (contentError != null) {
            _uiState.update { it.copy(contentError = contentError) }
            return
        }
        _uiState.update { it.copy(isAnalyzing = true, result = null) }
        viewModelScope.launch {
            val result = scanRepository.analyzeMessage(_uiState.value.content.trim())
            _uiState.update { it.copy(isAnalyzing = false) }
            when (result) {
                is ApiResult.Success -> _uiState.update { it.copy(result = result.data) }
                is ApiResult.Failure -> _events.send(OneShotEvent.ShowMessage(result.error.userMessage))
            }
        }
    }
}
