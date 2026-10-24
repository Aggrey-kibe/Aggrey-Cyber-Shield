package com.aggreyshield.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.InputValidator
import com.aggreyshield.app.core.common.Loadable
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.toLoadable
import com.aggreyshield.app.domain.model.MfaSetup
import com.aggreyshield.app.domain.repository.AuthRepository
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

data class MfaSetupUiState(
    val setup: Loadable<MfaSetup> = Loadable.Loading,
    val code: String = "",
    val codeError: String? = null,
    val isConfirming: Boolean = false,
    val confirmed: Boolean = false,
)

@HiltViewModel
class MfaSetupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MfaSetupUiState())
    val uiState: StateFlow<MfaSetupUiState> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onRetry() = load()
    fun onCodeChanged(value: String) = _uiState.update { it.copy(code = value, codeError = null) }

    fun onConfirm() {
        val error = InputValidator.validateMfaCode(_uiState.value.code)
        if (error != null) {
            _uiState.update { it.copy(codeError = error) }
            return
        }
        _uiState.update { it.copy(isConfirming = true) }
        viewModelScope.launch {
            val result = authRepository.confirmMfa(_uiState.value.code.trim())
            _uiState.update { it.copy(isConfirming = false) }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(confirmed = true) }
                    _events.send(OneShotEvent.ShowMessage("Multi-factor authentication is now enabled."))
                }
                is ApiResult.Failure -> {
                    val fieldError = result.error.fieldError("code")
                    if (fieldError != null) {
                        _uiState.update { it.copy(codeError = fieldError) }
                    } else {
                        _events.send(OneShotEvent.ShowMessage(result.error.userMessage))
                    }
                }
            }
        }
    }

    private fun load() {
        _uiState.update { it.copy(setup = Loadable.Loading) }
        viewModelScope.launch {
            _uiState.update { it.copy(setup = authRepository.startMfaSetup().toLoadable()) }
        }
    }
}
