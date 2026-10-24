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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.ui.components.BackTopBar
import com.aggreyshield.app.core.ui.components.StateContent
import com.aggreyshield.app.domain.model.CourseStatus
import com.aggreyshield.app.domain.model.TrainingCourse

@Composable
fun TrainingListScreen(viewModel: TrainingListViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            BackTopBar(
                title = "Training center",
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::onViewResultsClicked) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = "My results")
                    }
                },
            )
        },
    ) { padding ->
        StateContent(
            state = state,
            onRetry = viewModel::onRetry,
            isEmpty = { it.isEmpty() },
            emptyTitle = "No courses yet",
            emptyMessage = "Your organization hasn't assigned any training courses yet.",
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { courses ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(courses, key = { it.id }) { course ->
                    CourseCard(course, onClick = { viewModel.onCourseClicked(course.id) })
                }
            }
        }
    }
}

@Composable
private fun CourseCard(course: TrainingCourse, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = course.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    text = when (course.status) {
                        CourseStatus.COMPLETED -> "Completed"
                        CourseStatus.IN_PROGRESS -> "In progress"
                        CourseStatus.NOT_STARTED -> "Not started"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = course.topic.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (course.description != null) {
                Text(text = course.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            }
            LinearProgressIndicator(
                progress = { course.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(
                    text = "${course.progressPercent}% complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (course.durationMinutes != null) {
                    Text(
                        text = "${course.durationMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
