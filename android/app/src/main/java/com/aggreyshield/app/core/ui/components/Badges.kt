package com.aggreyshield.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aggreyshield.app.domain.model.AlertStatus
import com.aggreyshield.app.domain.model.IncidentStatus
import com.aggreyshield.app.domain.model.Severity
import com.aggreyshield.app.core.ui.theme.SeverityCritical
import com.aggreyshield.app.core.ui.theme.SeverityHigh
import com.aggreyshield.app.core.ui.theme.SeverityLow
import com.aggreyshield.app.core.ui.theme.SeverityMedium
import com.aggreyshield.app.core.ui.theme.SeverityUnknown
import com.aggreyshield.app.core.ui.theme.StatusClosed
import com.aggreyshield.app.core.ui.theme.StatusInProgress
import com.aggreyshield.app.core.ui.theme.StatusOpen
import com.aggreyshield.app.core.ui.theme.StatusResolved

fun Severity.color(): Color = when (this) {
    Severity.LOW -> SeverityLow
    Severity.MEDIUM -> SeverityMedium
    Severity.HIGH -> SeverityHigh
    Severity.CRITICAL -> SeverityCritical
    Severity.UNKNOWN -> SeverityUnknown
}

@Composable
private fun Badge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = modifier
            .background(color = color, shape = RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
fun SeverityBadge(severity: Severity, modifier: Modifier = Modifier) {
    Badge(text = severity.label.uppercase(), color = severity.color(), modifier = modifier)
}

@Composable
fun IncidentStatusBadge(status: IncidentStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        IncidentStatus.OPEN -> StatusOpen
        IncidentStatus.INVESTIGATING, IncidentStatus.CONTAINED -> StatusInProgress
        IncidentStatus.RESOLVED -> StatusResolved
        IncidentStatus.CLOSED -> StatusClosed
        IncidentStatus.UNKNOWN -> SeverityUnknown
    }
    Badge(text = status.label.uppercase(), color = color, modifier = modifier)
}

@Composable
fun AlertStatusBadge(status: AlertStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        AlertStatus.OPEN -> StatusOpen
        AlertStatus.ACKNOWLEDGED -> StatusInProgress
        AlertStatus.RESOLVED -> StatusResolved
        AlertStatus.UNKNOWN -> SeverityUnknown
    }
    Badge(text = status.label.uppercase(), color = color, modifier = modifier)
}
