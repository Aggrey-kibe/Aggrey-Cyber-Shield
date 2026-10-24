package com.aggreyshield.app.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.ui.components.BackTopBar
import com.aggreyshield.app.core.ui.components.SecondaryButton
import com.aggreyshield.app.core.ui.components.StateContent
import com.aggreyshield.app.core.ui.theme.SeverityCritical
import com.aggreyshield.app.core.ui.theme.SeverityLow
import com.aggreyshield.app.domain.model.User

@Composable
fun AccountSecurityScreen(viewModel: AccountSecurityViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = "Account security", onBack = onBack) }) { padding ->
        StateContent(
            state = state,
            onRetry = viewModel::onRetry,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { profile ->
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                SecurityStatusRow(
                    title = "Email verification",
                    isGood = profile.emailVerified,
                    goodText = "Verified",
                    badText = "Not verified",
                    action = if (!profile.emailVerified) {
                        { SecondaryButton(text = "Resend code", onClick = viewModel::onResendVerification) }
                    } else null,
                )
                SecurityStatusRow(
                    title = "Multi-factor authentication",
                    isGood = profile.mfaEnabled,
                    goodText = "Enabled",
                    badText = "Not enabled",
                    action = { SecondaryButton(text = if (profile.mfaEnabled) "Manage" else "Set up", onClick = viewModel::onMfaSetupClicked) },
                )
                Card(onClick = viewModel::onChangePasswordClicked, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        Text(text = "Change password", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityStatusRow(
    title: String,
    isGood: Boolean,
    goodText: String,
    badText: String,
    action: (@Composable () -> Unit)?,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGood) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (isGood) SeverityLow else SeverityCritical,
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (isGood) goodText else badText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isGood) SeverityLow else SeverityCritical,
                    )
                }
            }
            if (action != null) {
                Row(modifier = Modifier.padding(top = 10.dp)) { action() }
            }
        }
    }
}
