package com.aggreyshield.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.InputValidator
import com.aggreyshield.app.core.common.Loadable
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.toLoadable
import com.aggreyshield.app.domain.model.User
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

data class ProfileUiState(
    val profile: Loadable<User> = Loadable.Loading,
    val isEditing: Boolean = false,
    val editedName: String = "",
    val nameError: String? = null,
    val isSaving: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onEditClicked() {
        val current = (_uiState.value.profile as? Loadable.Loaded)?.data ?: return
        _uiState.update { it.copy(isEditing = true, editedName = current.fullName.orEmpty(), nameError = null) }
    }

    fun onCancelEdit() = _uiState.update { it.copy(isEditing = false) }

    fun onNameChanged(value: String) = _uiState.update { it.copy(editedName = value, nameError = null) }

    fun onSaveName() {
        val error = InputValidator.validateFullName(_uiState.value.editedName)
        if (error != null) {
            _uiState.update { it.copy(nameError = error) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = userRepository.updateProfile(_uiState.value.editedName)
            _uiState.update { it.copy(isSaving = false) }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isEditing = false, profile = Loadable.Loaded(result.data)) }
                    _events.send(OneShotEvent.ShowMessage("Profile updated."))
                }
                is ApiResult.Failure -> _events.send(OneShotEvent.ShowMessage(result.error.userMessage))
            }
        }
    }

    fun onAccountSecurityClicked() = navigate(Routes.ACCOUNT_SECURITY)
    fun onOrganizationClicked() = navigate(Routes.ORGANIZATION)
    fun onTrainingClicked() = navigate(Routes.TRAINING_LIST)
    fun onSettingsClicked() = navigate(Routes.SETTINGS)
    fun onAboutClicked() = navigate(Routes.ABOUT)
    fun onLogoutClicked() = navigate(Routes.LOGOUT_CONFIRM)

    private fun navigate(route: String) {
        viewModelScope.launch { _events.send(OneShotEvent.Navigate(route)) }
    }

    private fun load() {
        _uiState.update { it.copy(profile = Loadable.Loading) }
        viewModelScope.launch {
            _uiState.update { it.copy(profile = userRepository.getProfile().toLoadable()) }
        }
    }
}
