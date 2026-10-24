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
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.ui.components.BackTopBar
import com.aggreyshield.app.core.ui.components.PrimaryButton
import com.aggreyshield.app.core.ui.components.StateContent
import com.aggreyshield.app.domain.model.CourseDetail
import com.aggreyshield.app.domain.model.Lesson
import com.aggreyshield.app.domain.model.QuizQuestion

@Composable
fun CourseDetailScreen(viewModel: CourseDetailViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(title = "Course", onBack = onBack) }) { padding ->
        StateContent(
            state = state.course,
            onRetry = viewModel::onRetry,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { detail ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Text(text = detail.course.title, style = MaterialTheme.typography.headlineMedium)
                    if (detail.course.description != null) {
                        Text(
                            text = detail.course.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }

                if (detail.lessons.isNotEmpty()) {
                    item { Text(text = "Lessons", style = MaterialTheme.typography.titleMedium) }
                    items(detail.lessons, key = { "lesson-${it.id}" }) { lesson ->
                        LessonRow(
                            lesson = lesson,
                            isCompleting = state.completingLessonId == lesson.id,
                            onToggle = { viewModel.onLessonToggle(lesson.id) },
                        )
                    }
                }

                if (detail.quiz.isNotEmpty()) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(text = "Quiz", style = MaterialTheme.typography.titleMedium)
                    }
                    items(detail.quiz, key = { "quiz-${it.id}" }) { question ->
                        QuizQuestionCard(
                            question = question,
                            selectedOptionId = state.selectedAnswers[question.id],
                            onAnswerSelected = { optionId -> viewModel.onAnswerSelected(question.id, optionId) },
                        )
                    }
                    item {
                        PrimaryButton(
                            text = "Submit quiz",
                            onClick = viewModel::onSubmitQuiz,
                            isLoading = state.isSubmittingQuiz,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    val result = state.submittedResult ?: detail.lastResult
                    if (result != null) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = if (result.passed) "Passed" else "Not passed",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (result.passed) com.aggreyshield.app.core.ui.theme.SeverityLow else com.aggreyshield.app.core.ui.theme.SeverityCritical,
                                    )
                                    Text(
                                        text = "Score: ${result.scorePercent}%" + if (result.correct != null && result.total != null) " (${result.correct}/${result.total})" else "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonRow(lesson: Lesson, isCompleting: Boolean, onToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
            Icon(
                imageVector = if (lesson.completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (lesson.completed) "Completed" else "Not completed",
                tint = if (lesson.completed) com.aggreyshield.app.core.ui.theme.SeverityLow else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(text = lesson.title, style = MaterialTheme.typography.titleSmall)
                if (lesson.content != null) {
                    Text(
                        text = lesson.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (!lesson.completed) {
                com.aggreyshield.app.core.ui.components.SecondaryButton(
                    text = if (isCompleting) "..." else "Mark done",
                    onClick = onToggle,
                    enabled = !isCompleting,
                )
            }
        }
    }
}

@Composable
private fun QuizQuestionCard(question: QuizQuestion, selectedOptionId: String?, onAnswerSelected: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = question.text, style = MaterialTheme.typography.titleSmall)
            question.options.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = selectedOptionId == option.id, onClick = { onAnswerSelected(option.id) })
                        .padding(vertical = 4.dp),
                ) {
                    RadioButton(selected = selectedOptionId == option.id, onClick = { onAnswerSelected(option.id) })
                    Text(text = option.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}
