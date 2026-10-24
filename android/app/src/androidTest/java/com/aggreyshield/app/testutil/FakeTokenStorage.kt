package com.aggreyshield.app.testutil

import com.aggreyshield.app.core.security.AuthTokens
import com.aggreyshield.app.core.security.TokenStorage

/** In-memory TokenStorage for instrumented tests. Never touches the Android Keystore. */
class FakeTokenStorage : TokenStorage {
    private var tokens: AuthTokens? = null

    override fun saveTokens(tokens: AuthTokens) {
        this.tokens = tokens
    }

    override fun getAccessToken(): String? = tokens?.accessToken
    override fun getRefreshToken(): String? = tokens?.refreshToken

    override fun clear() {
        tokens = null
    }
}
