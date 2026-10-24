package com.aggreyshield.app.navigation

import androidx.lifecycle.ViewModel
import com.aggreyshield.app.core.network.NetworkMonitor
import com.aggreyshield.app.core.security.SessionManager
import com.aggreyshield.app.core.security.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * App-wide state the navigation shell reacts to. Session restoration itself happens once in
 * AggreyShieldApplication.onCreate(), before any UI is shown, so this ViewModel only observes.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    sessionManager: SessionManager,
    networkMonitor: NetworkMonitor,
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = sessionManager.state
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
}
