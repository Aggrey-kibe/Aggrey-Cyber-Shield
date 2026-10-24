package com.aggreyshield.app.presentation.auth

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
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

data class ResetPasswordUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
) {
    val passwordStrength: PasswordStrength
        get() = InputValidator.passwordStrength(newPassword)
}

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val resetToken: String =
        Uri.decode(
            savedStateHandle
                .get<String>(Routes.ARG_RESET_TOKEN)
                .orEmpty()
        )

    private val _uiState = MutableStateFlow(
        ResetPasswordUiState()
    )
    val uiState: StateFlow<ResetPasswordUiState> =
        _uiState.asStateFlow()

    private val _events =
        Channel<OneShotEvent>(Channel.BUFFERED)

    val events: Flow<OneShotEvent> =
        _events.receiveAsFlow()

    fun onNewPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                newPassword = value,
                passwordError = null,
            )
        }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                confirmPassword = value,
                confirmPasswordError = null,
            )
        }
    }

    fun onSubmit() {
        val state = _uiState.value

        if (resetToken.isBlank()) {
            viewModelScope.launch {
                _events.send(
                    OneShotEvent.ShowMessage(
                        "This password reset link is invalid or incomplete."
                    )
                )
            }
            return
        }

        val passwordError =
            InputValidator.validateNewPassword(
                state.newPassword
            )

        val confirmPasswordError =
            InputValidator.validatePasswordMatch(
                state.newPassword,
                state.confirmPassword
            )

        if (
            passwordError != null ||
            confirmPasswordError != null
        ) {
            _uiState.update {
                it.copy(
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                )
            }
            return
        }

        _uiState.update {
            it.copy(isLoading = true)
        }

        viewModelScope.launch {
            val result =
                authRepository.resetPassword(
                    resetToken,
                    state.newPassword,
                )

            _uiState.update {
                it.copy(isLoading = false)
            }

            when (result) {
                is ApiResult.Success -> {
                    _events.send(
                        OneShotEvent.ShowMessage(
                            "Password reset successfully. Please sign in with your new password."
                        )
                    )

                    _events.send(
                        OneShotEvent.Navigate(
                            Routes.LOGIN,
                            popUpToRoute = Routes.LOGIN,
                            inclusive = false,
                        )
                    )
                }

                is ApiResult.Failure -> {
                    val fieldError =
                        result.error.fieldError("token")

                    if (fieldError != null) {
                        _events.send(
                            OneShotEvent.ShowMessage(
                                fieldError
                            )
                        )
                    } else {
                        _events.send(
                            OneShotEvent.ShowMessage(
                                result.error.userMessage
                            )
                        )
                    }
                }
            }
        }
    }
}