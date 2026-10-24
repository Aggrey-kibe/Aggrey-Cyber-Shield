package com.aggreyshield.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aggreyshield.app.core.common.DateTimeUtils
import com.aggreyshield.app.core.ui.components.SeverityBadge
import com.aggreyshield.app.core.ui.components.StateContent
import com.aggreyshield.app.core.ui.components.color
import com.aggreyshield.app.domain.model.Incident
import com.aggreyshield.app.domain.model.ScanHistoryItem
import com.aggreyshield.app.domain.model.SecurityAlert
import com.aggreyshield.app.domain.model.TrainingCourse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AGGREY SHIELD") },
                actions = {
                    IconButton(onClick = viewModel::onNotificationsClicked) {
                        Icon(Icons.Filled.NotificationsNone, contentDescription = "Notifications")
                    }
                },
            )
        },
    ) { padding ->
        StateContent(
            state = state,
            onRetry = viewModel::onRetry,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { data ->
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Text(
                        text = "Welcome back, ${data.profile.displayName}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                item { SecurityScoreCard(score = data.securityScore.score, level = data.securityScore.level, onClick = viewModel::onSecurityScoreClicked) }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        QuickActionCard(
                            title = "Scan now",
                            icon = Icons.Filled.Search,
                            modifier = Modifier.weight(1f),
                            onClick = viewModel::onScannerClicked,
                        )
                        QuickActionCard(
                            title = "Report incident",
                            icon = Icons.Filled.ReportProblem,
                            modifier = Modifier.weight(1f),
                            onClick = viewModel::onReportIncidentClicked,
                        )
                    }
                }

                item {
                    SectionHeader(title = "Recent alerts", onViewAll = viewModel::onViewAllAlertsClicked)
                }
                if (data.recentAlerts.isEmpty()) {
                    item { EmptySectionRow("No recent alerts.") }
                } else {
                    items(data.recentAlerts, key = { "alert-${it.id}" }) { alert ->
                        AlertRow(alert)
                    }
                }

                item {
                    SectionHeader(title = "Recent incidents", onViewAll = viewModel::onViewAllIncidentsClicked)
                }
                if (data.recentIncidents.isEmpty()) {
                    item { EmptySectionRow("No incidents reported yet.") }
                } else {
                    items(data.recentIncidents, key = { "incident-${it.id}" }) { incident ->
                        IncidentRow(incident, onClick = { viewModel.onIncidentClicked(incident.id) })
                    }
                }

                item {
                    SectionHeader(title = "Recent scans", onViewAll = viewModel::onScannerClicked)
                }
                if (data.recentScans.isEmpty()) {
                    item { EmptySectionRow("No scans yet. Try the URL or message scanner.") }
                } else {
                    items(data.recentScans, key = { "scan-${it.id}" }) { scan ->
                        ScanRow(scan)
                    }
                }

                item {
                    SectionHeader(title = "Training progress", onViewAll = viewModel::onTrainingClicked)
                }
                if (data.trainingCourses.isEmpty()) {
                    item { EmptySectionRow("No training courses assigned yet.") }
                } else {
                    items(data.trainingCourses.take(3), key = { "course-${it.id}" }) { course ->
                        TrainingProgressRow(course)
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityScoreCard(score: Int, level: com.aggreyshield.app.domain.model.Severity, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(20.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(level.color().copy(alpha = 0.18f)),
            ) {
                Text(text = "$score", style = MaterialTheme.typography.titleLarge, color = level.color())
            }
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(text = "Security score", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${level.label} risk level",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun QuickActionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SectionHeader(title: String, onViewAll: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "View all",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onViewAll),
        )
    }
}

@Composable
private fun EmptySectionRow(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

@Composable
private fun AlertRow(alert: SecurityAlert) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = alert.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                SeverityBadge(alert.severity)
            }
            if (alert.description != null) {
                Text(
                    text = alert.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun IncidentRow(incident: Incident, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = incident.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = incident.category.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SeverityBadge(incident.severity)
        }
    }
}

@Composable
private fun ScanRow(scan: ScanHistoryItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
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

@Composable
private fun TrainingProgressRow(course: TrainingCourse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = course.title, style = MaterialTheme.typography.titleSmall)
            androidx.compose.material3.LinearProgressIndicator(
                progress = { course.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Text(
                text = "${course.progressPercent}% complete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
