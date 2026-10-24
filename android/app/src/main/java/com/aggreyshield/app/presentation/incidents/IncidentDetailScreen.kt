package com.aggreyshield.app.presentation.incidents

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
import com.aggreyshield.app.core.ui.components.IncidentStatusBadge
import com.aggreyshield.app.core.ui.components.PrimaryButton
import com.aggreyshield.app.core.ui.components.SeverityBadge
import com.aggreyshield.app.core.ui.components.StateContent
import com.aggreyshield.app.domain.model.Incident
import com.aggreyshield.app.domain.model.IncidentComment
import com.aggreyshield.app.domain.model.TimelineEntry

@Composable
fun IncidentDetailScreen(viewModel: IncidentDetailViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = "Incident", onBack = onBack) }) { padding ->
        StateContent(
            state = state.incident,
            onRetry = viewModel::onRetry,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { incident ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item { IncidentHeaderCard(incident) }

                if (incident.evidence.isNotEmpty()) {
                    item { Text(text = "Evidence", style = MaterialTheme.typography.titleMedium) }
                    items(incident.evidence) { evidence ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = evidence.fileName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }

                if (incident.timeline.isNotEmpty()) {
                    item { Text(text = "Timeline", style = MaterialTheme.typography.titleMedium) }
                    items(incident.timeline) { entry -> TimelineRow(entry) }
                }

                item { Text(text = "Comments", style = MaterialTheme.typography.titleMedium) }
                if (incident.comments.isEmpty()) {
                    item {
                        Text(
                            text = "No comments yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(incident.comments) { comment -> CommentRow(comment) }
                }

                item {
                    AggreyTextField(
                        value = state.commentText,
                        onValueChange = viewModel::onCommentChanged,
                        label = "Add a comment",
                        errorText = state.commentError,
                        singleLine = false,
                        minLines = 2,
                    )
                    PrimaryButton(
                        text = "Post comment",
                        onClick = viewModel::onPostComment,
                        isLoading = state.isPostingComment,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun IncidentHeaderCard(incident: Incident) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = incident.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                SeverityBadge(incident.severity)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                IncidentStatusBadge(incident.status)
            }
            if (incident.description != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(text = incident.description, style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(text = "Category: ${incident.category.label}", style = MaterialTheme.typography.bodySmall)
            if (incident.reporter != null) {
                Text(text = "Reported by: ${incident.reporter}", style = MaterialTheme.typography.bodySmall)
            }
            if (incident.assignedAnalyst != null) {
                Text(text = "Assigned to: ${incident.assignedAnalyst}", style = MaterialTheme.typography.bodySmall)
            }
            if (incident.createdAt != null) {
                Text(text = "Reported: ${DateTimeUtils.format(incident.createdAt)}", style = MaterialTheme.typography.bodySmall)
            }
            if (incident.resolution != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(text = "Resolution", style = MaterialTheme.typography.titleSmall)
                Text(text = incident.resolution, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun TimelineRow(entry: TimelineEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = entry.title, style = MaterialTheme.typography.titleSmall)
            if (entry.detail != null) {
                Text(text = entry.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (entry.at != null) {
                Text(
                    text = DateTimeUtils.format(entry.at),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CommentRow(comment: IncidentComment) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = comment.author ?: "Unknown", style = MaterialTheme.typography.titleSmall)
                if (comment.createdAt != null) {
                    Text(
                        text = DateTimeUtils.format(comment.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(text = comment.body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
