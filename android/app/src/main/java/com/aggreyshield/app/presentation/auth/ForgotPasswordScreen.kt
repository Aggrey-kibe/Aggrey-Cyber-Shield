package com.aggreyshield.app.presentation.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.ui.components.AggreyTextField
import com.aggreyshield.app.core.ui.components.BackTopBar
import com.aggreyshield.app.core.ui.components.PrimaryButton

@Composable
fun ForgotPasswordScreen(viewModel: ForgotPasswordViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = "Forgot password", onBack = onBack) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text(
                text = "Enter your account email. If it's registered, we'll send you a code to reset your password.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            AggreyTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChanged,
                label = "Email",
                errorText = state.emailError,
                keyboardType = KeyboardType.Email,
                enabled = !state.requestSent,
            )

            if (!state.requestSent) {
                PrimaryButton(
                    text = "Send reset code",
                    onClick = viewModel::onSubmit,
                    isLoading = state.isLoading,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                )
            } else {
                PrimaryButton(
                    text = "I have my code",
                    onClick = viewModel::onContinueToReset,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                )
            }
        }
    }
}
