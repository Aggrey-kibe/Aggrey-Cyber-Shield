package com.aggreyshield.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.InputValidator
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.PasswordStrength
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

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
) {
    val passwordStrength: PasswordStrength get() = InputValidator.passwordStrength(password)
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    fun onFullNameChanged(value: String) = _uiState.update { it.copy(fullName = value, fullNameError = null) }
    fun onEmailChanged(value: String) = _uiState.update { it.copy(email = value, emailError = null) }
    fun onPasswordChanged(value: String) = _uiState.update { it.copy(password = value, passwordError = null) }
    fun onConfirmPasswordChanged(value: String) = _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null) }

    fun onSubmit() {
        val state = _uiState.value
        val fullNameError = InputValidator.validateFullName(state.fullName)
        val emailError = InputValidator.validateEmail(state.email)
        val passwordError = InputValidator.validateNewPassword(state.password)
        val confirmPasswordError = InputValidator.validatePasswordMatch(state.password, state.confirmPassword)

        if (fullNameError != null || emailError != null || passwordError != null || confirmPasswordError != null) {
            _uiState.update {
                it.copy(
                    fullNameError = fullNameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = authRepository.register(state.email, state.password, state.fullName)
            _uiState.update { it.copy(isLoading = false) }
            when (result) {
                is ApiResult.Success -> {
                    _events.send(
                        OneShotEvent.Navigate(
                            Routes.verifyEmail(state.email),
                            popUpToRoute = Routes.REGISTER,
                            inclusive = true,
                        ),
                    )
                }
                is ApiResult.Failure -> {
                    val fieldError = result.error.fieldError("email")
                    if (fieldError != null) {
                        _uiState.update { it.copy(emailError = fieldError) }
                    } else {
                        _events.send(OneShotEvent.ShowMessage(result.error.userMessage))
                    }
                }
            }
        }
    }
}
