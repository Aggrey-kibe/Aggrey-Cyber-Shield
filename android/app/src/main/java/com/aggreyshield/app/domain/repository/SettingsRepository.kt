package com.aggreyshield.app.domain.repository

import com.aggreyshield.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Non-sensitive app preferences only. Tokens and personal data are never stored here. */
interface SettingsRepository {
    val onboardingCompleted: Flow<Boolean>
    val themeMode: Flow<ThemeMode>
    val hideInRecents: Flow<Boolean>
    val serverUrlOverride: Flow<String?>

    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setHideInRecents(hide: Boolean)
    suspend fun setServerUrlOverride(url: String?)
}
