package com.aggreyshield.app.presentation.profile

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.ui.components.AggreyPasswordField
import com.aggreyshield.app.core.ui.components.BackTopBar
import com.aggreyshield.app.core.ui.components.PrimaryButton

@Composable
fun ChangePasswordScreen(viewModel: ChangePasswordViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = "Change password", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            AggreyPasswordField(
                value = state.currentPassword,
                onValueChange = viewModel::onCurrentPasswordChanged,
                label = "Current password",
                errorText = state.currentPasswordError,
            )
            AggreyPasswordField(
                value = state.newPassword,
                onValueChange = viewModel::onNewPasswordChanged,
                label = "New password",
                errorText = state.newPasswordError,
                supportingText = "At least 10 characters, with uppercase, lowercase, and a number.",
                modifier = Modifier.padding(top = 12.dp),
            )
            if (state.newPassword.isNotEmpty()) {
                val strength = state.passwordStrength
                LinearProgressIndicator(progress = { strength.fraction }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                Text(text = strength.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AggreyPasswordField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChanged,
                label = "Confirm new password",
                errorText = state.confirmPasswordError,
                modifier = Modifier.padding(top = 12.dp),
            )
            PrimaryButton(
                text = "Change password",
                onClick = viewModel::onSubmit,
                isLoading = state.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
        }
    }
}
