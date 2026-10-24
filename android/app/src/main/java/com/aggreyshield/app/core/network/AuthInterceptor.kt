package com.aggreyshield.app.core.network

import com.aggreyshield.app.core.security.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/** Adds "Authorization: Bearer <token>" to every request except those marked with the no-auth header. */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        if (original.header(ApiEndpoints.HEADER_NO_AUTH) != null) {
            return chain.proceed(original.newBuilder().removeHeader(ApiEndpoints.HEADER_NO_AUTH).build())
        }

        val token = sessionManager.accessToken() ?: return chain.proceed(original)
        return chain.proceed(original.newBuilder().header("Authorization", "Bearer $token").build())
    }
}
