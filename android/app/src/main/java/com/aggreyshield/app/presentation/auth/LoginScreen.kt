package com.aggreyshield.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.ui.components.AggreyPasswordField
import com.aggreyshield.app.core.ui.components.AggreyTextField
import com.aggreyshield.app.core.ui.components.PrimaryButton
import com.aggreyshield.app.core.ui.components.TextLinkButton

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "AGGREY SHIELD", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Sign in to your account",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp, top = 4.dp),
        )

        AggreyTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChanged,
            label = "Email",
            errorText = state.emailError,
            keyboardType = KeyboardType.Email,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        AggreyPasswordField(
            value = state.password,
            onValueChange = viewModel::onPasswordChanged,
            label = "Password",
            errorText = state.passwordError,
        )

        if (state.showMfaField) {
            AggreyTextField(
                value = state.mfaCode,
                onValueChange = viewModel::onMfaCodeChanged,
                label = "Authenticator code",
                keyboardType = KeyboardType.NumberPassword,
                supportingText = "Your account has multi-factor authentication enabled.",
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        TextLinkButton(
            text = "Forgot password?",
            onClick = onNavigateToForgotPassword,
            modifier = Modifier.padding(top = 4.dp),
        )

        PrimaryButton(
            text = "Sign in",
            onClick = viewModel::onSubmit,
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )

        Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Don't have an account?", style = MaterialTheme.typography.bodyMedium)
            TextLinkButton(text = "Create one", onClick = onNavigateToRegister)
        }
    }
}
