package com.aggreyshield.app.presentation.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.common.DateTimeUtils
import com.aggreyshield.app.core.ui.components.AlertStatusBadge
import com.aggreyshield.app.core.ui.components.PrimaryButton
import com.aggreyshield.app.core.ui.components.SeverityBadge
import com.aggreyshield.app.core.ui.components.StateContent
import com.aggreyshield.app.domain.model.AlertStatus
import com.aggreyshield.app.domain.model.SecurityAlert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(viewModel: AlertsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Security alerts") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.selectedStatus == null,
                        onClick = { viewModel.onStatusSelected(null) },
                        label = { Text("All") },
                    )
                }
                items(AlertStatus.entries.filter { it != AlertStatus.UNKNOWN }) { status ->
                    FilterChip(
                        selected = state.selectedStatus == status,
                        onClick = { viewModel.onStatusSelected(status) },
                        label = { Text(status.label) },
                    )
                }
            }

            StateContent(
                state = state.alerts,
                onRetry = viewModel::onRetry,
                isEmpty = { it.isEmpty() },
                emptyTitle = "No alerts",
                emptyMessage = "You're all caught up. New security alerts will appear here.",
                modifier = Modifier.fillMaxSize(),
            ) { alerts ->
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(alerts, key = { it.id }) { alert ->
                        AlertCard(
                            alert = alert,
                            isAcknowledging = state.acknowledgingId == alert.id,
                            onAcknowledge = { viewModel.onAcknowledge(alert.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: SecurityAlert, isAcknowledging: Boolean, onAcknowledge: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = alert.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                SeverityBadge(alert.severity)
            }
            if (alert.description != null) {
                Text(
                    text = alert.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Column {
                    AlertStatusBadge(alert.status)
                    if (alert.createdAt != null) {
                        Text(
                            text = DateTimeUtils.format(alert.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                if (alert.status == AlertStatus.OPEN) {
                    PrimaryButton(text = "Acknowledge", onClick = onAcknowledge, isLoading = isAcknowledging)
                }
            }
        }
    }
}
