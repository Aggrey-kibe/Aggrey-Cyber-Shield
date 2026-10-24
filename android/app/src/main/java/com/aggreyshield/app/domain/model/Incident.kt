package com.aggreyshield.app.domain.model

import java.time.Instant

enum class IncidentCategory(val label: String) {
    PHISHING("Phishing"),
    FRAUD("Fraud"),
    ACCOUNT_COMPROMISE("Account compromise"),
    LOST_DEVICE("Lost device"),
    MALWARE("Malware"),
    SUSPICIOUS_PAYMENT("Suspicious payment"),
    DATA_BREACH("Data breach"),
    OTHER("Other");

    companion object {
        fun fromApi(value: String?): IncidentCategory =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: OTHER
    }
}

enum class IncidentStatus(val label: String) {
    OPEN("Open"),
    INVESTIGATING("Investigating"),
    CONTAINED("Contained"),
    RESOLVED("Resolved"),
    CLOSED("Closed"),
    UNKNOWN("Unknown");

    companion object {
        fun fromApi(value: String?): IncidentStatus =
            entries.firstOrNull { it != UNKNOWN && it.name.equals(value?.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

data class TimelineEntry(val title: String, val detail: String?, val actor: String?, val at: Instant?)

data class IncidentComment(val id: String, val author: String?, val body: String, val createdAt: Instant?)

data class EvidenceItem(val id: String, val fileName: String, val contentType: String?, val sizeBytes: Long?)

data class Incident(
    val id: String,
    val title: String,
    val description: String?,
    val category: IncidentCategory,
    val severity: Severity,
    val status: IncidentStatus,
    val reporter: String?,
    val assignedAnalyst: String?,
    val resolution: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val timeline: List<TimelineEntry>,
    val comments: List<IncidentComment>,
    val evidence: List<EvidenceItem>,
)

/** What the user fills in on the report form. */
data class NewIncident(
    val title: String,
    val description: String,
    val category: IncidentCategory,
    val severity: Severity,
)

/** A file the user picked as evidence. [uri] is a content:// URI string; the file is only read when uploading. */
data class EvidenceFileInfo(val uri: String, val name: String, val sizeBytes: Long, val mimeType: String)

/** Client-side limits for evidence files. The server enforces its own limits as well. */
object EvidenceRules {
    const val MAX_BYTES: Long = 10L * 1024 * 1024
    val ALLOWED_MIME_TYPES: Set<String> = setOf("image/jpeg", "image/png", "image/webp", "application/pdf", "text/plain")

    /** Returns a message when the file is not acceptable, or null when it is. */
    fun validate(info: EvidenceFileInfo): String? = when {
        info.sizeBytes <= 0L -> "That file is empty."
        info.sizeBytes > MAX_BYTES -> "Evidence files can be at most 10 MB."
        info.mimeType !in ALLOWED_MIME_TYPES -> "Only images (JPEG, PNG, WebP), PDF and text files are accepted."
        else -> null
    }
}
