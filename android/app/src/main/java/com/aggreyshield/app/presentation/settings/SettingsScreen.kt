package com.aggreyshield.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.ui.components.AggreyTextField
import com.aggreyshield.app.core.ui.components.BackTopBar
import com.aggreyshield.app.core.ui.components.SecondaryButton
import com.aggreyshield.app.domain.model.ThemeMode

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val connectionTest by viewModel.connectionTest.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = "Settings", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(text = "Appearance", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.themeMode == mode,
                        onClick = { viewModel.onThemeModeSelected(mode) },
                        label = { Text(mode.label) },
                    )
                }
            }

            Text(text = "Privacy", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Hide app content in Recents", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Blocks screenshots and hides this app's preview in the app switcher.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.hideInRecents, onCheckedChange = viewModel::onHideInRecentsToggled)
            }

            Text(text = "Connection", style = MaterialTheme.typography.titleMedium)

            if (viewModel.allowServerOverride) {
                AggreyTextField(
                    value = state.serverUrlOverride,
                    onValueChange = viewModel::onServerUrlOverrideChanged,
                    label = "Server URL override (debug only)",
                    supportingText = "Example: http://192.168.1.50:8000/ for a phone on the same Wi-Fi network. Leave blank to use the built-in address.",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            SecondaryButton(
                text = "Test connection",
                onClick = viewModel::onTestConnection,
                modifier = Modifier.padding(top = 12.dp),
            )

            when (val test = connectionTest) {
                is ConnectionTestState.Idle -> Unit
                is ConnectionTestState.Testing -> Text(
                    text = "Testing...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                is ConnectionTestState.Result -> Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Connected to ${test.health.serverUrl}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Database: ${if (test.health.databaseConnected) "connected" else "unavailable"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (test.health.environment != null) {
                            Text(
                                text = "Environment: ${test.health.environment}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                is ConnectionTestState.Error -> Text(
                    text = test.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
