package com.aggreyshield.app.presentation.auth

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.AppError
import com.aggreyshield.app.core.security.SessionManager
import com.aggreyshield.app.testutil.FakeAuthRepository
import com.aggreyshield.app.testutil.FakeTokenStorage
import com.aggreyshield.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun newViewModel(authRepository: FakeAuthRepository = FakeAuthRepository()): LoginViewModel {
        val sessionManager = SessionManager(FakeTokenStorage())
        sessionManager.restore()
        return LoginViewModel(authRepository, sessionManager)
    }

    @Test
    fun `submitting blank fields shows validation errors and never calls the repository`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = newViewModel(repository)

        viewModel.onSubmit()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.emailError)
        assertNotNull(viewModel.uiState.value.passwordError)
        assertNull(repository.lastLoginEmail)
    }

    @Test
    fun `submitting valid credentials calls the repository with trimmed email`() = runTest {
        val repository = FakeAuthRepository().apply { nextResult = ApiResult.Success(Unit) }
        val viewModel = newViewModel(repository)

        viewModel.onEmailChanged("  user@example.com  ")
        viewModel.onPasswordChanged("Password123")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals("user@example.com", repository.lastLoginEmail)
        assertEquals("Password123", repository.lastLoginPassword)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `MFA_REQUIRED failure reveals the authenticator code field`() = runTest {
        val repository = FakeAuthRepository().apply {
            nextResult = ApiResult.Failure(AppError.Unauthorized(code = "MFA_REQUIRED"))
        }
        val viewModel = newViewModel(repository)

        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("Password123")
        assertFalse(viewModel.uiState.value.showMfaField)

        viewModel.onSubmit()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showMfaField)
    }

    @Test
    fun `a plain unauthorized failure does not reveal the MFA field`() = runTest {
        val repository = FakeAuthRepository().apply {
            nextResult = ApiResult.Failure(AppError.Unauthorized(serverMessage = "Invalid credentials."))
        }
        val viewModel = newViewModel(repository)

        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("WrongPassword1")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showMfaField)
    }
}
