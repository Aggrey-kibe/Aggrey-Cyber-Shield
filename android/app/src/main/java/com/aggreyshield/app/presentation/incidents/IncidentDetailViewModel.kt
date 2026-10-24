package com.aggreyshield.app.presentation.incidents

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.InputValidator
import com.aggreyshield.app.core.common.Loadable
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.toLoadable
import com.aggreyshield.app.domain.model.Incident
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

data class IncidentDetailUiState(
    val incident: Loadable<Incident> = Loadable.Loading,
    val commentText: String = "",
    val commentError: String? = null,
    val isPostingComment: Boolean = false,
)

@HiltViewModel
class IncidentDetailViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val incidentId: String = Uri.decode(savedStateHandle.get<String>(Routes.ARG_INCIDENT_ID).orEmpty())

    private val _uiState = MutableStateFlow(IncidentDetailUiState())
    val uiState: StateFlow<IncidentDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onCommentChanged(value: String) = _uiState.update { it.copy(commentText = value, commentError = null) }

    fun onPostComment() {
        val error = InputValidator.validateComment(_uiState.value.commentText)
        if (error != null) {
            _uiState.update { it.copy(commentError = error) }
            return
        }
        _uiState.update { it.copy(isPostingComment = true) }
        viewModelScope.launch {
            val result = incidentRepository.addComment(incidentId, _uiState.value.commentText)
            _uiState.update { it.copy(isPostingComment = false) }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(commentText = "") }
                    load()
                }
                is ApiResult.Failure -> _events.send(OneShotEvent.ShowMessage(result.error.userMessage))
            }
        }
    }

    private fun load() {
        _uiState.update { it.copy(incident = Loadable.Loading) }
        viewModelScope.launch {
            _uiState.update { it.copy(incident = incidentRepository.get(incidentId).toLoadable()) }
        }
    }
}
