package com.aggreyshield.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.Loadable
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.toLoadable
import com.aggreyshield.app.domain.model.SecurityScore
import com.aggreyshield.app.domain.repository.UserRepository
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
class SecurityScoreViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<Loadable<SecurityScore>>(Loadable.Loading)
    val uiState: StateFlow<Loadable<SecurityScore>> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        _uiState.value = Loadable.Loading
        viewModelScope.launch {
            _uiState.value = userRepository.getSecurityScore().toLoadable()
        }
    }
}
