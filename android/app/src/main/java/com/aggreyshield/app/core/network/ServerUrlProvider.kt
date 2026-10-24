package com.aggreyshield.app.core.network

import com.aggreyshield.app.di.ApplicationScope
import com.aggreyshield.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the optional developer server-URL override. It is only ever read in DEBUG builds
 * (see BaseUrlInterceptor); release and staging builds always use the URL compiled into BuildConfig.
 */
@Singleton
class ServerUrlProvider @Inject constructor(
    settings: SettingsRepository,
    @ApplicationScope scope: CoroutineScope,
) {
    @Volatile
    private var override: HttpUrl? = null

    init {
        scope.launch {
            settings.serverUrlOverride.collect { value ->
                override = value?.toHttpUrlOrNull()
            }
        }
    }

    fun currentOverride(): HttpUrl? = override
}
