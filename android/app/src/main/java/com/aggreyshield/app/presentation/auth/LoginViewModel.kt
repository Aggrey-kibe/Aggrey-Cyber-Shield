package com.aggreyshield.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.AppError
import com.aggreyshield.app.core.common.InputValidator
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.security.SessionManager
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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val mfaCode: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val showMfaField: Boolean = false,
    val isLoading: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    init {
        // A forced logout (expired/invalid refresh token) lands the user back here.
        // Voluntary logout does not set this notice, so it stays silent.
        if (sessionManager.consumeExpiredNotice()) {
            viewModelScope.launch {
                _events.send(OneShotEvent.ShowMessage("Your session expired. Please sign in again."))
            }
        }
    }

    fun onEmailChanged(value: String) = _uiState.update { it.copy(email = value, emailError = null) }
    fun onPasswordChanged(value: String) = _uiState.update { it.copy(password = value, passwordError = null) }
    fun onMfaCodeChanged(value: String) = _uiState.update { it.copy(mfaCode = value) }

    fun onSubmit() {
        val state = _uiState.value
        val emailError = InputValidator.validateEmail(state.email)
        val passwordError = InputValidator.validateLoginPassword(state.password)
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val mfaCode = state.mfaCode.trim().takeIf { it.isNotEmpty() }
            val result = authRepository.login(state.email.trim(), state.password, mfaCode)
            _uiState.update { it.copy(isLoading = false) }

            when (result) {
                is ApiResult.Success -> Unit // Session becomes AUTHENTICATED; navigation is handled globally.
                is ApiResult.Failure -> handleFailure(result.error)
            }
        }
    }

    private fun handleFailure(error: AppError) {
        val requiresMfa = (error as? AppError.Unauthorized)?.code == "MFA_REQUIRED" ||
            (error as? AppError.Client)?.code == "MFA_REQUIRED"
        if (requiresMfa) {
            _uiState.update { it.copy(showMfaField = true) }
            viewModelScope.launch {
                _events.send(OneShotEvent.ShowMessage("Enter the 6-digit code from your authenticator app."))
            }
            return
        }
        viewModelScope.launch { _events.send(OneShotEvent.ShowMessage(error.userMessage)) }
    }
}
