package com.aggreyshield.app.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DEBUG BUILDS ONLY.
 *
 * If the user has configured a server URL override, requests are redirected
 * to that server while preserving the original request path and query.
 *
 * Example:
 *   Built-in: http://10.0.2.2:8000/api/v1/auth/login
 *   Override: http://192.168.180.112:8001/
 *
 * Result:
 *   http://192.168.180.112:8001/api/v1/auth/login
 */
@Singleton
class BaseUrlInterceptor @Inject constructor(
    private val serverUrlProvider: ServerUrlProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val overrideUrl = serverUrlProvider.currentOverride()

        if (overrideUrl == null) {
            return chain.proceed(request)
        }

        val originalUrl = request.url

        val newUrl = originalUrl.newBuilder()
            .scheme(overrideUrl.scheme)
            .host(overrideUrl.host)
            .port(overrideUrl.port)
            .build()

        return chain.proceed(
            request.newBuilder()
                .url(newUrl)
                .build()
        )
    }
}