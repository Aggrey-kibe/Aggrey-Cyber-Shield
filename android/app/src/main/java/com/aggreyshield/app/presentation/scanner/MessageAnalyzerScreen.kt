package com.aggreyshield.app.presentation.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.common.DateTimeUtils
import com.aggreyshield.app.core.ui.components.AggreyTextField
import com.aggreyshield.app.core.ui.components.BackTopBar
import com.aggreyshield.app.core.ui.components.PrimaryButton
import com.aggreyshield.app.core.ui.components.SeverityBadge
import com.aggreyshield.app.domain.model.MessageAnalysis

@Composable
fun MessageAnalyzerScreen(viewModel: MessageAnalyzerViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = "Message analyzer", onBack = onBack) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            item {
                Text(
                    text = "Paste a suspicious SMS, email, WhatsApp, or social media message to check it for phishing and scam indicators.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                AggreyTextField(
                    value = state.content,
                    onValueChange = viewModel::onContentChanged,
                    label = "Message content",
                    errorText = state.contentError,
                    singleLine = false,
                    minLines = 5,
                )
            }
            item {
                PrimaryButton(
                    text = "Analyze",
                    onClick = viewModel::onAnalyze,
                    isLoading = state.isAnalyzing,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.result?.let { result ->
                item { MessageResultCard(result) }
            }
        }
    }
}

@Composable
private fun MessageResultCard(result: MessageAnalysis) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Risk level", style = MaterialTheme.typography.titleMedium)
                SeverityBadge(result.riskLevel)
            }

            if (result.analyzedAt != null) {
                Text(
                    text = "Analyzed ${DateTimeUtils.format(result.analyzedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (result.recommendation != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(text = "Recommendation", style = MaterialTheme.typography.titleSmall)
                Text(text = result.recommendation, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }

            if (result.explanation != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(text = "Explanation", style = MaterialTheme.typography.titleSmall)
                Text(text = result.explanation, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }

            if (result.suspiciousPhrases.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(text = "Suspicious phrases found", style = MaterialTheme.typography.titleSmall)
                result.suspiciousPhrases.forEach { phrase ->
                    Text(text = "\u2022 \u201C$phrase\u201D", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }

            if (result.indicators.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(text = "Indicators", style = MaterialTheme.typography.titleSmall)
                result.indicators.forEach { indicator ->
                    Text(
                        text = "\u2022 ${indicator.name}${indicator.description?.let { " \u2014 $it" } ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
