package com.aggreyshield.app.testutil

import com.aggreyshield.app.core.network.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeNetworkMonitor(online: Boolean = true) : NetworkMonitor {
    private val _isOnline = MutableStateFlow(online)
    override val isOnline: StateFlow<Boolean> = _isOnline
    override fun isCurrentlyOnline(): Boolean = _isOnline.value
}
