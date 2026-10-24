package com.aggreyshield.app.domain.model

import java.time.Instant

enum class ScanKind { URL, MESSAGE, UNKNOWN;
    companion object {
        fun fromApi(value: String?): ScanKind =
            entries.firstOrNull { it != UNKNOWN && it.name.equals(value?.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

data class ScanIndicator(val name: String, val description: String?, val severity: Severity)

data class DomainInfo(
    val domain: String?,
    val registrar: String?,
    val createdAt: Instant?,
    val ageDays: Int?,
    val country: String?,
)

data class ReputationEntry(val provider: String, val verdict: String?, val detail: String?)

/** Result of a URL scan, exactly as reported by the backend. Missing data stays null or empty; nothing is filled in locally. */
data class ScanResult(
    val id: String?,
    val target: String?,
    val riskLevel: Severity,
    val riskScore: Double?,
    val classification: String?,
    val confidence: Double?,
    val indicators: List<ScanIndicator>,
    val reasons: List<String>,
    val recommendation: String?,
    val scannedAt: Instant?,
    val domainInfo: DomainInfo?,
    val redirects: List<String>,
    val reputation: List<ReputationEntry>,
    val modelVersion: String?,
)

/** Result of a message analysis, exactly as reported by the backend. */
data class MessageAnalysis(
    val id: String?,
    val riskLevel: Severity,
    val riskScore: Double?,
    val classification: String?,
    val confidence: Double?,
    val indicators: List<ScanIndicator>,
    val suspiciousPhrases: List<String>,
    val recommendation: String?,
    val explanation: String?,
    val analyzedAt: Instant?,
    val modelVersion: String?,
)

data class ScanHistoryItem(
    val id: String,
    val kind: ScanKind,
    val summary: String?,
    val riskLevel: Severity,
    val riskScore: Double?,
    val scannedAt: Instant?,
)
