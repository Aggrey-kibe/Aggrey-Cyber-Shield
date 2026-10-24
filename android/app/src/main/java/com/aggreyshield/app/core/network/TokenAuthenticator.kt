package com.aggreyshield.app.core.network

import com.aggreyshield.app.core.security.SessionManager
import com.aggreyshield.app.data.api.TokenRefreshApi
import com.aggreyshield.app.data.model.RefreshRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Called by OkHttp when the server answers 401 to a request that carried an access token.
 * It refreshes the tokens once (other threads wait), then retries the request. If the
 * refresh token is rejected, the session ends and the app returns to the login screen.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val refreshApi: TokenRefreshApi,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val failedToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()
            ?: return null

        if (responseCount(response) >= 2) return null

        return synchronized(lock) { refreshLocked(response, failedToken) }
    }

    private fun refreshLocked(response: Response, failedToken: String): Request? {
        // Another request may already have refreshed the tokens while this one waited.
        val currentToken = sessionManager.accessToken()
        if (currentToken != null && currentToken != failedToken) {
            return retryWith(response, currentToken)
        }

        val refreshToken = sessionManager.refreshToken()
        if (refreshToken == null) {
            sessionManager.endSession(expired = true)
            return null
        }

        return try {
            val tokens = runBlocking { refreshApi.refresh(RefreshRequestDto(refreshToken)) }
            sessionManager.updateTokens(tokens.accessToken, tokens.refreshToken ?: refreshToken)
            retryWith(response, tokens.accessToken)
        } catch (e: HttpException) {
            if (e.code() in 400..403) sessionManager.endSession(expired = true)
            null
        } catch (e: IOException) {
            null
        } catch (e: RuntimeException) {
            // For example an unreadable refresh response. The original 401 is passed on to the caller.
            null
        }
    }

    private fun retryWith(response: Response, token: String): Request =
        response.request.newBuilder().header("Authorization", "Bearer $token").build()

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
