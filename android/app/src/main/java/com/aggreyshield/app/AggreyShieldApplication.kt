package com.aggreyshield.app

import android.app.Application
import com.aggreyshield.app.core.network.NetworkMonitor
import com.aggreyshield.app.core.security.SessionManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AggreyShieldApplication : Application() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var networkMonitor: NetworkMonitor

    override fun onCreate() {
        super.onCreate()
        // Restored once, here, before any screen (including Splash) reads session state.
        sessionManager.restore()
        // Referencing it forces this @Singleton to be created now, so its network callback
        // is registered from app start rather than on first use.
        networkMonitor.isCurrentlyOnline()
    }
}
