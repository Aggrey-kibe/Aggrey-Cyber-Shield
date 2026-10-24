package com.aggreyshield.app.presentation.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.aggreyshield.app.core.ui.components.StateContent
import com.aggreyshield.app.core.ui.theme.SeverityCritical
import com.aggreyshield.app.core.ui.theme.SeverityLow
import com.aggreyshield.app.domain.model.QuizResult

@Composable
fun TrainingResultsScreen(viewModel: TrainingResultsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = "Training results", onBack = onBack) }) { padding ->
        StateContent(
            state = state,
            onRetry = viewModel::onRetry,
            isEmpty = { it.isEmpty() },
            emptyTitle = "No results yet",
            emptyMessage = "Complete a course quiz to see your results here.",
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { results ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(results, key = { "${it.courseId}-${it.completedAt}" }) { result -> ResultRow(result) }
            }
        }
    }
}

@Composable
private fun ResultRow(result: QuizResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = result.courseTitle ?: "Course",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (result.passed) "Passed" else "Not passed",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (result.passed) SeverityLow else SeverityCritical,
                )
            }
            Text(
                text = "Score: ${result.scorePercent}%" + if (result.correct != null && result.total != null) " (${result.correct}/${result.total})" else "",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (result.completedAt != null) {
                Text(
                    text = DateTimeUtils.format(result.completedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
