package com.aggreyshield.app.presentation.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.aggreyshield.app.core.ui.components.PrimaryButton
import com.aggreyshield.app.core.ui.components.SeverityBadge
import com.aggreyshield.app.core.ui.components.color
import com.aggreyshield.app.domain.model.ScanIndicator
import com.aggreyshield.app.domain.model.ScanResult

@Composable
fun UrlScannerScreen(viewModel: UrlScannerViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = "URL scanner", onBack = onBack) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            item {
                Text(
                    text = "Paste a link to check it against AGGREY SHIELD's threat analysis before you open it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                AggreyTextField(
                    value = state.url,
                    onValueChange = viewModel::onUrlChanged,
                    label = "URL or domain",
                    errorText = state.urlError,
                    keyboardType = KeyboardType.Uri,
                )
            }
            item {
                PrimaryButton(
                    text = "Scan",
                    onClick = viewModel::onScan,
                    isLoading = state.isScanning,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.result?.let { result ->
                item { ScanResultCard(result) }
            }
        }
    }
}

@Composable
private fun ScanResultCard(result: ScanResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Risk level", style = MaterialTheme.typography.titleMedium)
                SeverityBadge(result.riskLevel)
            }

            if (result.riskScore != null) {
                Text(
                    text = "Score: ${"%.0f".format(result.riskScore)}/100",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (result.scannedAt != null) {
                Text(
                    text = "Scanned ${DateTimeUtils.format(result.scannedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (result.recommendation != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(text = "Recommendation", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = result.recommendation,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (result.reasons.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(text = "Reasons", style = MaterialTheme.typography.titleSmall)
                result.reasons.forEach { reason ->
                    BulletLine(reason)
                }
            }

            if (result.indicators.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(text = "Indicators", style = MaterialTheme.typography.titleSmall)
                result.indicators.forEach { indicator -> IndicatorRow(indicator) }
            }

            if (result.domainInfo != null) {
                val info = result.domainInfo
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(text = "Domain information", style = MaterialTheme.typography.titleSmall)
                info.domain?.let { KeyValueLine("Domain", it) }
                info.registrar?.let { KeyValueLine("Registrar", it) }
                info.country?.let { KeyValueLine("Country", it) }
                info.ageDays?.let { KeyValueLine("Domain age", "$it days") }
            }

            if (result.redirects.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(text = "Redirects", style = MaterialTheme.typography.titleSmall)
                result.redirects.forEach { redirect -> BulletLine(redirect) }
            }

            if (result.reputation.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(text = "Reputation sources", style = MaterialTheme.typography.titleSmall)
                result.reputation.forEach { entry ->
                    KeyValueLine(entry.provider, entry.verdict ?: "No verdict reported")
                }
            }
        }
    }
}

@Composable
private fun IndicatorRow(indicator: ScanIndicator) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Icon(
            Icons.Filled.Circle,
            contentDescription = null,
            tint = indicator.severity.color(),
            modifier = Modifier.padding(top = 5.dp).size(8.dp),
        )
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(text = indicator.name, style = MaterialTheme.typography.bodyMedium)
            if (indicator.description != null) {
                Text(
                    text = indicator.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(text = "\u2022  ", style = MaterialTheme.typography.bodyMedium)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun KeyValueLine(key: String, value: String) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(text = key, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
