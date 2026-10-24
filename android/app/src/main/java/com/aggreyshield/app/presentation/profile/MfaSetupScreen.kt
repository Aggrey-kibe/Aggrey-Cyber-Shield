package com.aggreyshield.app.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.ui.components.AggreyTextField
import com.aggreyshield.app.core.ui.components.BackTopBar
import com.aggreyshield.app.core.ui.components.PrimaryButton
import com.aggreyshield.app.core.ui.components.StateContent
import com.aggreyshield.app.core.ui.theme.SeverityLow
import com.aggreyshield.app.domain.model.MfaSetup

@Composable
fun MfaSetupScreen(viewModel: MfaSetupViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = "Multi-factor authentication", onBack = onBack) }) { padding ->
        StateContent(
            state = state.setup,
            onRetry = viewModel::onRetry,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { setup ->
            if (state.confirmed) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SeverityLow)
                    Text(
                        text = "Multi-factor authentication is enabled.",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text(
                        text = "Scan this into your authenticator app, or enter the key manually, then enter the 6-digit code it generates.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SetupKeyCard(setup)
                    AggreyTextField(
                        value = state.code,
                        onValueChange = viewModel::onCodeChanged,
                        label = "6-digit code",
                        errorText = state.codeError,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    PrimaryButton(
                        text = "Confirm",
                        onClick = viewModel::onConfirm,
                        isLoading = state.isConfirming,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupKeyCard(setup: MfaSetup) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Setup key", style = MaterialTheme.typography.titleSmall)
            Text(
                text = setup.secret,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.padding(top = 6.dp),
            )
            if (setup.otpauthUri != null) {
                Text(
                    text = "Or use this setup link in your authenticator app:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = setup.otpauthUri,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
