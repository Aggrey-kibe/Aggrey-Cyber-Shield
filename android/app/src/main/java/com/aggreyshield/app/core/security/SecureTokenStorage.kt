package com.aggreyshield.app.core.security

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the access and refresh tokens encrypted with a Keystore-backed AES-GCM key.
 * The SharedPreferences file only ever contains ciphertext, never a readable token.
 * If encryption is unavailable on a device the tokens are kept in memory only, so the user
 * simply has to sign in again after the app process ends. They are never written in plain text.
 */
@Singleton
class SecureTokenStorage @Inject constructor(
    @ApplicationContext context: Context,
) : TokenStorage {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val cipher = KeystoreCipher(KEY_ALIAS)

    private var cachedAccessToken: String? = null
    private var cachedRefreshToken: String? = null
    private var loaded = false

    @Synchronized
    override fun saveTokens(tokens: AuthTokens) {
        cachedAccessToken = tokens.accessToken
        cachedRefreshToken = tokens.refreshToken
        loaded = true
        try {
            preferences.edit()
                .putString(KEY_ACCESS, cipher.encrypt(tokens.accessToken))
                .putString(KEY_REFRESH, cipher.encrypt(tokens.refreshToken))
                .apply()
        } catch (e: Exception) {
            preferences.edit().clear().apply()
        }
    }

    @Synchronized
    override fun getAccessToken(): String? {
        ensureLoaded()
        return cachedAccessToken
    }

    @Synchronized
    override fun getRefreshToken(): String? {
        ensureLoaded()
        return cachedRefreshToken
    }

    @Synchronized
    override fun clear() {
        cachedAccessToken = null
        cachedRefreshToken = null
        loaded = true
        preferences.edit().clear().apply()
    }

    private fun ensureLoaded() {
        if (loaded) return
        val access = readDecrypted(KEY_ACCESS)
        val refresh = readDecrypted(KEY_REFRESH)
        if (access == null || refresh == null) {
            // Missing or unreadable data: treat as signed out and remove whatever is left.
            preferences.edit().clear().apply()
            cachedAccessToken = null
            cachedRefreshToken = null
        } else {
            cachedAccessToken = access
            cachedRefreshToken = refresh
        }
        loaded = true
    }

    private fun readDecrypted(key: String): String? {
        val encrypted = preferences.getString(key, null) ?: return null
        return cipher.decrypt(encrypted)
    }

    private companion object {
        const val PREFS_NAME = "aggrey_shield_secure_session"
        const val KEY_ALIAS = "aggrey_shield_session_key"
        const val KEY_ACCESS = "access_token_enc"
        const val KEY_REFRESH = "refresh_token_enc"
    }
}
