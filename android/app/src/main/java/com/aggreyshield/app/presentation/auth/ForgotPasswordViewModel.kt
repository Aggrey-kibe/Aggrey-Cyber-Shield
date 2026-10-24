package com.aggreyshield.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val requestSent: Boolean = false,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    fun onEmailChanged(value: String) = _uiState.update { it.copy(email = value, emailError = null) }

    fun onSubmit() {
        val emailError = InputValidator.validateEmail(_uiState.value.email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val email = _uiState.value.email
            val result = authRepository.forgotPassword(email)
            _uiState.update { it.copy(isLoading = false) }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(requestSent = true) }
                    _events.send(OneShotEvent.ShowMessage("If that email is registered, a reset code has been sent."))
                }
                is ApiResult.Failure -> _events.send(OneShotEvent.ShowMessage(result.error.userMessage))
            }
        }
    }

    fun onContinueToReset() {
        viewModelScope.launch {
            _events.send(OneShotEvent.Navigate(Routes.resetPassword(_uiState.value.email)))
        }
    }
}
