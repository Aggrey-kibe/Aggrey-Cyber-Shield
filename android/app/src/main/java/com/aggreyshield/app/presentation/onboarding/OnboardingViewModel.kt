package com.aggreyshield.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.domain.repository.SettingsRepository
import com.aggreyshield.app.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingPage(val title: String, val message: String)

val ONBOARDING_PAGES = listOf(
    OnboardingPage(
        title = "Know your real risk",
        message = "AGGREY SHIELD scans URLs and messages against real backend analysis, so you know what's actually dangerous, not a guess.",
    ),
    OnboardingPage(
        title = "Respond to incidents fast",
        message = "Report phishing, fraud, compromised accounts, and more. Track every incident from open to resolved.",
    ),
    OnboardingPage(
        title = "Build lasting security habits",
        message = "Short, practical training on phishing, passwords, MFA, and safe browsing for you and your organization.",
    ),
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    fun onGetStartedClicked() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
            _events.send(OneShotEvent.Navigate(Routes.LOGIN, popUpToRoute = Routes.ONBOARDING, inclusive = true))
        }
    }
}
