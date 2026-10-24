package com.aggreyshield.app.presentation.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.aggreyshield.app.core.common.DateTimeUtils
import com.aggreyshield.app.core.ui.components.SeverityBadge
import com.aggreyshield.app.core.ui.components.StateContent
import com.aggreyshield.app.domain.model.ScanHistoryItem
import com.aggreyshield.app.domain.model.ScanKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerHubScreen(viewModel: ScannerHubViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Threat scanner") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                ScannerOptionCard(
                    title = "Scan a URL",
                    description = "Check a link before you open it",
                    icon = Icons.Filled.Language,
                    modifier = Modifier.weight(1f),
                    onClick = viewModel::onUrlScannerClicked,
                )
                ScannerOptionCard(
                    title = "Analyze a message",
                    description = "Check SMS, email or chat text",
                    icon = Icons.AutoMirrored.Filled.Message,
                    modifier = Modifier.weight(1f),
                    onClick = viewModel::onMessageAnalyzerClicked,
                )
            }

            Text(
                text = "Recent scans",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            StateContent(
                state = state,
                onRetry = viewModel::onRetry,
                isEmpty = { it.isEmpty() },
                emptyTitle = "No scans yet",
                emptyMessage = "Scan a URL or paste a suspicious message to see results here.",
                modifier = Modifier.fillMaxSize(),
            ) { scans ->
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(scans, key = { it.id }) { scan -> ScanHistoryRow(scan) }
                }
            }
        }
    }
}

@Composable
private fun ScannerOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun ScanHistoryRow(scan: ScanHistoryItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(14.dp),
        ) {
            Icon(
                imageVector = if (scan.kind == ScanKind.MESSAGE) Icons.AutoMirrored.Filled.Message else Icons.Filled.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(text = scan.summary ?: "Scan", style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(
                    text = DateTimeUtils.format(scan.scannedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SeverityBadge(scan.riskLevel)
        }
    }
}
