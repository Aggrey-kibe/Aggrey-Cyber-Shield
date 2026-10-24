package com.aggreyshield.app.presentation.organization

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.common.DateTimeUtils
import com.aggreyshield.app.core.ui.components.BackTopBar
import com.aggreyshield.app.core.ui.components.SecondaryButton
import com.aggreyshield.app.core.ui.components.StateContent

@Composable
fun OrganizationScreen(viewModel: OrganizationViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = "Organization", onBack = onBack) }) { padding ->
        StateContent(
            state = state,
            onRetry = viewModel::onRetry,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { organization ->
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = organization.name, style = MaterialTheme.typography.titleLarge)
                        if (organization.description != null) {
                            Text(
                                text = organization.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        if (organization.memberCount != null) {
                            Text(
                                text = "${organization.memberCount} members",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        if (organization.createdAt != null) {
                            Text(
                                text = "Since ${DateTimeUtils.format(organization.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                SecondaryButton(
                    text = "View members",
                    onClick = viewModel::onViewMembersClicked,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
            }
        }
    }
}
