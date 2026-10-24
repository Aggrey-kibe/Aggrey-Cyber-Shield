package com.aggreyshield.app.presentation.incidents

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import com.aggreyshield.app.core.ui.components.IncidentStatusBadge
import com.aggreyshield.app.core.ui.components.SeverityBadge
import com.aggreyshield.app.core.ui.components.StateContent
import com.aggreyshield.app.domain.model.Incident
import com.aggreyshield.app.domain.model.IncidentStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentListScreen(viewModel: IncidentListViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Incidents") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onReportClicked) {
                Icon(Icons.Filled.Add, contentDescription = "Report incident")
            }
        },
    ) { padding ->
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
                items(IncidentStatus.entries.filter { it != IncidentStatus.UNKNOWN }) { status ->
                    FilterChip(
                        selected = state.selectedStatus == status,
                        onClick = { viewModel.onStatusSelected(status) },
                        label = { Text(status.label) },
                    )
                }
            }

            StateContent(
                state = state.incidents,
                onRetry = viewModel::onRetry,
                isEmpty = { it.isEmpty() },
                emptyTitle = "No incidents",
                emptyMessage = "Nothing reported yet. Tap the + button to report a security incident.",
                modifier = Modifier.fillMaxSize(),
            ) { incidents ->
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(incidents, key = { it.id }) { incident ->
                        IncidentCard(incident, onClick = { viewModel.onIncidentClicked(incident.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentCard(incident: Incident, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = incident.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                SeverityBadge(incident.severity)
            }
            Text(
                text = incident.category.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                IncidentStatusBadge(incident.status)
                if (incident.reporter != null) {
                    Text(
                        text = "by ${incident.reporter}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
