package com.aggreyshield.app.presentation.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.InputValidator
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.domain.repository.AuthRepository
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

data class VerifyEmailUiState(
    val email: String = "",
    val code: String = "",
    val codeError: String? = null,
    val isSubmitting: Boolean = false,
    val isResending: Boolean = false,
)

@HiltViewModel
class VerifyEmailViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val email: String = Uri.decode(savedStateHandle.get<String>(Routes.ARG_EMAIL).orEmpty())

    private val _uiState = MutableStateFlow(VerifyEmailUiState(email = email))
    val uiState: StateFlow<VerifyEmailUiState> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    fun onCodeChanged(value: String) = _uiState.update { it.copy(code = value, codeError = null) }

    fun onSubmit() {
        val codeError = InputValidator.validateCode(_uiState.value.code)
        if (codeError != null) {
            _uiState.update { it.copy(codeError = codeError) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = authRepository.verifyEmail(email, _uiState.value.code)
            _uiState.update { it.copy(isSubmitting = false) }
            when (result) {
                is ApiResult.Success -> {
                    _events.send(OneShotEvent.ShowMessage("Email verified. Please sign in."))
                    _events.send(OneShotEvent.Navigate(Routes.LOGIN, popUpToRoute = Routes.LOGIN, inclusive = false))
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

    fun onResendCode() {
        _uiState.update { it.copy(isResending = true) }
        viewModelScope.launch {
            val result = authRepository.resendVerification(email)
            _uiState.update { it.copy(isResending = false) }
            val message = when (result) {
                is ApiResult.Success -> "A new code was sent to $email."
                is ApiResult.Failure -> result.error.userMessage
            }
            _events.send(OneShotEvent.ShowMessage(message))
        }
    }
}
