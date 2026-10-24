package com.aggreyshield.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.Loadable
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.toLoadable
import com.aggreyshield.app.domain.model.User
import com.aggreyshield.app.domain.repository.AuthRepository
import com.aggreyshield.app.domain.repository.UserRepository
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

@HiltViewModel
class AccountSecurityViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<Loadable<User>>(Loadable.Loading)
    val uiState: StateFlow<Loadable<User>> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onChangePasswordClicked() {
        viewModelScope.launch { _events.send(OneShotEvent.Navigate(Routes.CHANGE_PASSWORD)) }
    }

    fun onMfaSetupClicked() {
        viewModelScope.launch { _events.send(OneShotEvent.Navigate(Routes.MFA_SETUP)) }
    }

    fun onResendVerification() {
        val email = (_uiState.value as? Loadable.Loaded)?.data?.email ?: return
        viewModelScope.launch {
            val result = authRepository.resendVerification(email)
            val message = when (result) {
                is ApiResult.Success -> "A new verification code was sent to $email."
                is ApiResult.Failure -> result.error.userMessage
            }
            _events.send(OneShotEvent.ShowMessage(message))
        }
    }

    private fun load() {
        _uiState.value = Loadable.Loading
        viewModelScope.launch {
            _uiState.value = userRepository.getProfile().toLoadable()
        }
    }
}
