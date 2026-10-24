package com.aggreyshield.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.InputValidator
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.PasswordStrength
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

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val isSaving: Boolean = false,
) {
    val passwordStrength: PasswordStrength get() = InputValidator.passwordStrength(newPassword)
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    fun onCurrentPasswordChanged(value: String) = _uiState.update { it.copy(currentPassword = value, currentPasswordError = null) }
    fun onNewPasswordChanged(value: String) = _uiState.update { it.copy(newPassword = value, newPasswordError = null) }
    fun onConfirmPasswordChanged(value: String) = _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null) }

    fun onSubmit() {
        val state = _uiState.value
        val currentError = InputValidator.validateLoginPassword(state.currentPassword)
        val newError = InputValidator.validateNewPassword(state.newPassword)
        val confirmError = InputValidator.validatePasswordMatch(state.newPassword, state.confirmPassword)

        if (currentError != null || newError != null || confirmError != null) {
            _uiState.update { it.copy(currentPasswordError = currentError, newPasswordError = newError, confirmPasswordError = confirmError) }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = authRepository.changePassword(state.currentPassword, state.newPassword)
            _uiState.update { it.copy(isSaving = false) }
            when (result) {
                is ApiResult.Success -> {
                    _events.send(OneShotEvent.ShowMessage("Password changed successfully."))
                    _events.send(OneShotEvent.NavigateBack)
                }
                is ApiResult.Failure -> {
                    val fieldError = result.error.fieldError("current_password")
                    if (fieldError != null) {
                        _uiState.update { it.copy(currentPasswordError = fieldError) }
                    } else {
                        _events.send(OneShotEvent.ShowMessage(result.error.userMessage))
                    }
                }
            }
        }
    }
}
