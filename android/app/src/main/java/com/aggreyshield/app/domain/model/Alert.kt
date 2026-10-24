package com.aggreyshield.app.domain.model

import java.time.Instant

enum class AlertStatus(val label: String) {
    OPEN("Open"),
    ACKNOWLEDGED("Acknowledged"),
    RESOLVED("Resolved"),
    UNKNOWN("Unknown");

    companion object {
        fun fromApi(value: String?): AlertStatus =
            entries.firstOrNull { it != UNKNOWN && it.name.equals(value?.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

data class SecurityAlert(
    val id: String,
    val title: String,
    val description: String?,
    val severity: Severity,
    val status: AlertStatus,
    val source: String?,
    val createdAt: Instant?,
)
