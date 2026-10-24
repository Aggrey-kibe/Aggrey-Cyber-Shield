package com.aggreyshield.app.data.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.AppError
import com.aggreyshield.app.core.network.ApiCaller
import com.aggreyshield.app.core.security.SessionManager
import com.aggreyshield.app.data.model.TokenResponseDto
import com.aggreyshield.app.testutil.FakeAuthApi
import com.aggreyshield.app.testutil.FakeNetworkMonitor
import com.aggreyshield.app.testutil.FakeTokenStorage
import com.aggreyshield.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * These tests protect a specific security-relevant behavior: the app must never consider
 * itself "logged in" unless the backend actually returned a refresh token.
 */
@ExperimentalCoroutinesApi
class AuthRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun newRepository(api: FakeAuthApi, sessionManager: SessionManager): AuthRepositoryImpl {
        val apiCaller = ApiCaller(FakeNetworkMonitor(online = true))
        return AuthRepositoryImpl(api, apiCaller, sessionManager)
    }

    @Test
    fun `login with a refresh token starts the session`() = runTest {
        val api = FakeAuthApi().apply {
            nextLoginResponse = TokenResponseDto(accessToken = "access-1", refreshToken = "refresh-1")
        }
        val sessionManager = SessionManager(FakeTokenStorage())
        val repository = newRepository(api, sessionManager)

        val result = repository.login("user@example.com", "Password123", null)

        assertTrue(result is ApiResult.Success)
        assertEquals("access-1", sessionManager.accessToken())
        assertEquals("refresh-1", sessionManager.refreshToken())
    }

    @Test
    fun `login response missing a refresh token never starts a session`() = runTest {
        val api = FakeAuthApi().apply {
            nextLoginResponse = TokenResponseDto(accessToken = "access-1", refreshToken = null)
        }
        val sessionManager = SessionManager(FakeTokenStorage())
        val repository = newRepository(api, sessionManager)

        val result = repository.login("user@example.com", "Password123", null)

        assertTrue(result is ApiResult.Failure)
        assertTrue((result as ApiResult.Failure).error is AppError.InvalidResponse)
        assertNull(sessionManager.accessToken())
    }

    @Test
    fun `email and password are trimmed and forwarded to the backend as sent`() = runTest {
        val api = FakeAuthApi()
        val sessionManager = SessionManager(FakeTokenStorage())
        val repository = newRepository(api, sessionManager)

        repository.login("  user@example.com  ", "Password123", "  ")

        assertEquals("user@example.com", api.lastLoginRequest?.email)
        assertEquals("Password123", api.lastLoginRequest?.password)
        assertNull(api.lastLoginRequest?.mfaCode)
    }

    @Test
    fun `logout always clears the local session even without network`() = runTest {
        val sessionManager = SessionManager(FakeTokenStorage())
        sessionManager.startSession("access-1", "refresh-1")
        val repository = newRepository(FakeAuthApi(), sessionManager)

        repository.logout()

        assertNull(sessionManager.accessToken())
        assertNull(sessionManager.refreshToken())
    }
}
