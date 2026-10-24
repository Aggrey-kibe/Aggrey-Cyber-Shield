package com.aggreyshield.app.presentation.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.Loadable
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.toLoadable
import com.aggreyshield.app.domain.model.Organization
import com.aggreyshield.app.domain.repository.UserRepository
import com.aggreyshield.app.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrganizationViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<Loadable<Organization>>(Loadable.Loading)
    val uiState: StateFlow<Loadable<Organization>> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onViewMembersClicked() {
        viewModelScope.launch { _events.send(OneShotEvent.Navigate(Routes.ORGANIZATION_MEMBERS)) }
    }

    private fun load() {
        _uiState.value = Loadable.Loading
        viewModelScope.launch {
            _uiState.value = userRepository.getOrganization().toLoadable()
        }
    }
}
