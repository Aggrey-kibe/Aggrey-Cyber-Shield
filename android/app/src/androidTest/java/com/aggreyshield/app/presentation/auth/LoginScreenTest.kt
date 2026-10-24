package com.aggreyshield.app.presentation.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.aggreyshield.app.core.security.SessionManager
import com.aggreyshield.app.core.ui.theme.AggreyShieldTheme
import com.aggreyshield.app.testutil.FakeAuthRepository
import com.aggreyshield.app.testutil.FakeTokenStorage
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Renders the real LoginScreen with a hand-built ViewModel (no Hilt test graph needed, since
 * ViewModels are supplied to screens by the caller rather than resolved with hiltViewModel()
 * inside the screen itself).
 */
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var authRepository: FakeAuthRepository

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
    }

    private fun setContent() {
        val sessionManager = SessionManager(FakeTokenStorage())
        sessionManager.restore()
        val viewModel = LoginViewModel(authRepository, sessionManager)

        composeTestRule.setContent {
            AggreyShieldTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onNavigateToRegister = {},
                    onNavigateToForgotPassword = {},
                )
            }
        }
    }

    @Test
    fun tappingSignInWithEmptyFieldsShowsValidationErrorsAndDoesNotCallTheRepository() {
        setContent()

        composeTestRule.onNodeWithText("Sign in").performClick()

        composeTestRule.onNodeWithText("Enter your email address.").assertExists()
        composeTestRule.onNodeWithText("Enter your password.").assertExists()
        assert(authRepository.loginCallCount == 0)
    }

    @Test
    fun enteringValidCredentialsAndTappingSignInCallsTheRepository() {
        setContent()

        composeTestRule.onNodeWithText("Email").performTextInput("user@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("Password123")
        composeTestRule.onNodeWithText("Sign in").performClick()

        composeTestRule.waitUntil(timeoutMillis = 2_000) { authRepository.loginCallCount == 1 }
        assert(authRepository.lastLoginEmail == "user@example.com")
    }
}
