package com.aggreyshield.app.presentation.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.common.PasswordStrength
import com.aggreyshield.app.core.ui.components.AggreyPasswordField
import com.aggreyshield.app.core.ui.components.AggreyTextField
import com.aggreyshield.app.core.ui.components.BackTopBar
import com.aggreyshield.app.core.ui.components.PrimaryButton

@Composable
fun RegisterScreen(viewModel: RegisterViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = "Create account", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(
                text = "Join AGGREY CYBER SHIELD",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            AggreyTextField(
                value = state.fullName,
                onValueChange = viewModel::onFullNameChanged,
                label = "Full name",
                errorText = state.fullNameError,
                modifier = Modifier.padding(bottom = 12.dp),
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
                supportingText = "At least 10 characters, with uppercase, lowercase, and a number.",
            )
            if (state.password.isNotEmpty()) {
                val strength = state.passwordStrength
                LinearProgressIndicator(
                    progress = { strength.fraction },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
                Text(
                    text = strength.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AggreyPasswordField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChanged,
                label = "Confirm password",
                errorText = state.confirmPasswordError,
                modifier = Modifier.padding(top = 12.dp),
            )

            PrimaryButton(
                text = "Create account",
                onClick = viewModel::onSubmit,
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
        }
    }
}
