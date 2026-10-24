package com.aggreyshield.app.core.security

data class AuthTokens(val accessToken: String, val refreshToken: String)

/** Where the login tokens live. The production implementation encrypts them with the Android Keystore. */
interface TokenStorage {
    fun saveTokens(tokens: AuthTokens)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun clear()
}
