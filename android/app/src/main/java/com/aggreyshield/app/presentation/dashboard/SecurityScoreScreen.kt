package com.aggreyshield.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.common.DateTimeUtils
import com.aggreyshield.app.core.ui.components.BackTopBar
import com.aggreyshield.app.core.ui.components.StateContent
import com.aggreyshield.app.core.ui.components.color
import com.aggreyshield.app.domain.model.ScoreFactor

@Composable
fun SecurityScoreScreen(viewModel: SecurityScoreViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = "Security score", onBack = onBack) }) { padding ->
        StateContent(
            state = state,
            onRetry = viewModel::onRetry,
            isEmpty = { it.factors.isEmpty() },
            emptyTitle = "No score factors yet",
            emptyMessage = "Your score isn't broken down into factors yet. Check back after your first scan or incident.",
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { score ->
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(96.dp).clip(CircleShape).background(score.level.color().copy(alpha = 0.18f)),
                        ) {
                            Text(text = "${score.score}", style = MaterialTheme.typography.headlineLarge, color = score.level.color())
                        }
                        Text(
                            text = "${score.level.label} risk level",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        if (score.computedAt != null) {
                            Text(
                                text = "Last computed ${DateTimeUtils.format(score.computedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    Text(text = "What's affecting your score", style = MaterialTheme.typography.titleMedium)
                }
                items(score.factors, key = { it.name }) { factor -> FactorRow(factor) }
            }
        }
    }
}

@Composable
private fun FactorRow(factor: ScoreFactor) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = factor.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (factor.impact != null) {
                    val sign = if (factor.impact >= 0) "+" else ""
                    Text(
                        text = "$sign${factor.impact}",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (factor.impact >= 0) com.aggreyshield.app.core.ui.theme.SeverityLow else com.aggreyshield.app.core.ui.theme.SeverityCritical,
                    )
                }
            }
            if (factor.description != null) {
                Text(
                    text = factor.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
