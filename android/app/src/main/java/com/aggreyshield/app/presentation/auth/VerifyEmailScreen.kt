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
import com.aggreyshield.app.core.ui.components.TextLinkButton

@Composable
fun VerifyEmailScreen(viewModel: VerifyEmailViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = "Verify your email", onBack = onBack) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text(
                text = "We sent a verification code to ${state.email}. Enter it below to activate your account.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            AggreyTextField(
                value = state.code,
                onValueChange = viewModel::onCodeChanged,
                label = "Verification code",
                errorText = state.codeError,
                keyboardType = KeyboardType.Text,
            )

            PrimaryButton(
                text = "Verify",
                onClick = viewModel::onSubmit,
                isLoading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )

            TextLinkButton(
                text = if (state.isResending) "Sending..." else "Resend code",
                onClick = viewModel::onResendCode,
                enabled = !state.isResending,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}
