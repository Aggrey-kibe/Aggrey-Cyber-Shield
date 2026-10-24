package com.aggreyshield.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aggreyshield.app.domain.model.ThemeMode
import com.aggreyshield.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HIDE_IN_RECENTS = booleanPreferencesKey("hide_in_recents")
        val SERVER_URL_OVERRIDE = stringPreferencesKey("server_url_override")
    }

    override val onboardingCompleted: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    override val themeMode: Flow<ThemeMode> =
        context.settingsDataStore.data.map { ThemeMode.fromStored(it[Keys.THEME_MODE]) }

    override val hideInRecents: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.HIDE_IN_RECENTS] ?: false }

    override val serverUrlOverride: Flow<String?> =
        context.settingsDataStore.data.map { it[Keys.SERVER_URL_OVERRIDE]?.takeIf { url -> url.isNotBlank() } }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.settingsDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setHideInRecents(hide: Boolean) {
        context.settingsDataStore.edit { it[Keys.HIDE_IN_RECENTS] = hide }
    }

    override suspend fun setServerUrlOverride(url: String?) {
        context.settingsDataStore.edit {
            if (url.isNullOrBlank()) it.remove(Keys.SERVER_URL_OVERRIDE) else it[Keys.SERVER_URL_OVERRIDE] = url.trim()
        }
    }
}
