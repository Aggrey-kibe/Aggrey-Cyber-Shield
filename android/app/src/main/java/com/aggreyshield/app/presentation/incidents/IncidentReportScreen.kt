package com.aggreyshield.app.presentation.incidents

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.ui.components.AggreyTextField
import com.aggreyshield.app.core.ui.components.BackTopBar
import com.aggreyshield.app.core.ui.components.PrimaryButton
import com.aggreyshield.app.core.ui.components.SecondaryButton
import com.aggreyshield.app.domain.model.IncidentCategory
import com.aggreyshield.app.domain.model.Severity

@Composable
fun IncidentReportScreen(viewModel: IncidentReportViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.onEvidenceSelected(uri?.toString())
    }

    Scaffold(topBar = { BackTopBar(title = "Report incident", onBack = onBack) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            item {
                AggreyTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChanged,
                    label = "Title",
                    errorText = state.titleError,
                )
            }
            item {
                AggreyTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChanged,
                    label = "Description",
                    errorText = state.descriptionError,
                    singleLine = false,
                    minLines = 4,
                )
            }
            item {
                Text(text = "Category", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    items(IncidentCategory.entries) { category ->
                        FilterChip(
                            selected = state.category == category,
                            onClick = { viewModel.onCategorySelected(category) },
                            label = { Text(category.label) },
                        )
                    }
                }
            }
            item {
                Text(text = "Severity", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    items(listOf(Severity.LOW, Severity.MEDIUM, Severity.HIGH, Severity.CRITICAL)) { severity ->
                        FilterChip(
                            selected = state.severity == severity,
                            onClick = { viewModel.onSeveritySelected(severity) },
                            label = { Text(severity.label) },
                        )
                    }
                }
            }
            item {
                Text(text = "Evidence (optional)", style = MaterialTheme.typography.titleSmall)
                if (state.evidence == null) {
                    SecondaryButton(
                        text = "Attach a file",
                        onClick = { filePicker.launch(arrayOf("image/*", "application/pdf", "text/plain")) },
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        text = "Images, PDF, or text files up to 10 MB.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                        ) {
                            Icon(Icons.Filled.Description, contentDescription = null)
                            Text(
                                text = state.evidence!!.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 10.dp).weight(1f),
                                maxLines = 1,
                            )
                            IconButton(onClick = viewModel::onRemoveEvidence) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove attachment")
                            }
                        }
                    }
                }
                if (state.evidenceError != null) {
                    Text(
                        text = state.evidenceError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            item {
                PrimaryButton(
                    text = "Submit report",
                    onClick = viewModel::onSubmit,
                    isLoading = state.isSubmitting,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}
