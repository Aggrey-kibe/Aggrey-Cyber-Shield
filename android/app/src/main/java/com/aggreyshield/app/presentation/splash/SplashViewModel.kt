package com.aggreyshield.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.security.SessionManager
import com.aggreyshield.app.core.security.SessionState
import com.aggreyshield.app.domain.repository.SettingsRepository
import com.aggreyshield.app.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Decides, once, where the app should land: Dashboard if a session is already restored,
 * otherwise Onboarding (first launch only) or Login. The session itself was already restored
 * in AggreyShieldApplication.onCreate(), so this only reads the result.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val destination = when (sessionManager.state.value) {
                SessionState.AUTHENTICATED -> Routes.DASHBOARD
                else -> {
                    val onboardingDone = settingsRepository.onboardingCompleted.first()
                    if (onboardingDone) Routes.LOGIN else Routes.ONBOARDING
                }
            }
            _events.send(OneShotEvent.Navigate(destination, popUpToRoute = Routes.SPLASH, inclusive = true))
        }
    }
}
