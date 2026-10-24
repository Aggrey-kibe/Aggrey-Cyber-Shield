package com.aggreyshield.app.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class SessionState { UNKNOWN, AUTHENTICATED, UNAUTHENTICATED }

/**
 * The single source of truth for "is the user signed in?".
 * The navigation layer observes [state] and sends the user to the login screen whenever the
 * session ends, whether by logout or because the server rejected the refresh token.
 */
@Singleton
class SessionManager @Inject constructor(
    private val tokenStorage: TokenStorage,
) {
    private val _state = MutableStateFlow(SessionState.UNKNOWN)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _expiredNotice = MutableStateFlow(false)
    val expiredNotice: StateFlow<Boolean> = _expiredNotice.asStateFlow()

    /** Reads stored tokens once at app start. */
    fun restore() {
        _state.value = if (tokenStorage.getRefreshToken() != null) {
            SessionState.AUTHENTICATED
        } else {
            SessionState.UNAUTHENTICATED
        }
    }

    fun startSession(accessToken: String, refreshToken: String) {
        tokenStorage.saveTokens(AuthTokens(accessToken, refreshToken))
        _expiredNotice.value = false
        _state.value = SessionState.AUTHENTICATED
    }

    fun updateTokens(accessToken: String, refreshToken: String) {
        tokenStorage.saveTokens(AuthTokens(accessToken, refreshToken))
    }

    fun accessToken(): String? = tokenStorage.getAccessToken()

    fun refreshToken(): String? = tokenStorage.getRefreshToken()

    /** Wipes stored tokens. [expired] = true shows a "session expired" notice on the login screen. */
    fun endSession(expired: Boolean) {
        tokenStorage.clear()
        if (expired) _expiredNotice.value = true
        _state.value = SessionState.UNAUTHENTICATED
    }

    /** Returns true once if a "session expired" notice is waiting, then clears it. */
    fun consumeExpiredNotice(): Boolean {
        val pending = _expiredNotice.value
        _expiredNotice.value = false
        return pending
    }
}
