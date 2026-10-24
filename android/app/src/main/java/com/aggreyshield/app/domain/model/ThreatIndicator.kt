package com.aggreyshield.app.domain.model

import java.time.Instant

enum class IndicatorType(val label: String) {
    DOMAIN("Domain"),
    URL("URL"),
    IP("IP address"),
    FILE_HASH("File hash"),
    PHISHING("Phishing"),
    OTHER("Other");

    companion object {
        fun fromApi(value: String?): IndicatorType =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: OTHER
    }
}

data class ThreatIndicator(
    val id: String,
    val type: IndicatorType,
    val value: String,
    val severity: Severity,
    val source: String?,
    val description: String?,
    val tags: List<String>,
    val firstSeen: Instant?,
    val lastSeen: Instant?,
)
