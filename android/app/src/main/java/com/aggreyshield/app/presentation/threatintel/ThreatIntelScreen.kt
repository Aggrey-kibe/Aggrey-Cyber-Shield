package com.aggreyshield.app.presentation.threatintel

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.common.DateTimeUtils
import com.aggreyshield.app.core.ui.components.AggreyTextField
import com.aggreyshield.app.core.ui.components.BackTopBar
import com.aggreyshield.app.core.ui.components.SeverityBadge
import com.aggreyshield.app.core.ui.components.StateContent
import com.aggreyshield.app.domain.model.IndicatorType
import com.aggreyshield.app.domain.model.ThreatIndicator

@Composable
fun ThreatIntelScreen(viewModel: ThreatIntelViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = "Threat intelligence", onBack = onBack) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                AggreyTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChanged,
                    label = "Search domain, IP, or hash",
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::onSearch) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.selectedType == null,
                        onClick = { viewModel.onTypeSelected(null) },
                        label = { Text("All") },
                    )
                }
                items(IndicatorType.entries) { type ->
                    FilterChip(
                        selected = state.selectedType == type,
                        onClick = { viewModel.onTypeSelected(type) },
                        label = { Text(type.label) },
                    )
                }
            }

            StateContent(
                state = state.indicators,
                onRetry = viewModel::onRetry,
                isEmpty = { it.isEmpty() },
                emptyTitle = "No indicators found",
                emptyMessage = "Try a different search term or filter.",
                modifier = Modifier.fillMaxSize(),
            ) { indicators ->
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(indicators, key = { it.id }) { indicator -> IndicatorCard(indicator) }
                }
            }
        }
    }
}

@Composable
private fun IndicatorCard(indicator: ThreatIndicator) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = indicator.value, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 2)
                SeverityBadge(indicator.severity)
            }
            Text(
                text = indicator.type.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (indicator.description != null) {
                Text(text = indicator.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            }
            if (indicator.source != null) {
                Text(
                    text = "Source: ${indicator.source}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (indicator.lastSeen != null) {
                Text(
                    text = "Last seen: ${DateTimeUtils.format(indicator.lastSeen)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (indicator.tags.isNotEmpty()) {
                Text(
                    text = indicator.tags.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
