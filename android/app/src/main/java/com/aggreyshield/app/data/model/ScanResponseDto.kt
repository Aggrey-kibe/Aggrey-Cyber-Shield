package com.aggreyshield.app.data.model

import com.aggreyshield.app.core.common.DateTimeUtils
import com.aggreyshield.app.domain.model.DomainInfo
import com.aggreyshield.app.domain.model.MessageAnalysis
import com.aggreyshield.app.domain.model.Page
import com.aggreyshield.app.domain.model.ReputationEntry
import com.aggreyshield.app.domain.model.ScanHistoryItem
import com.aggreyshield.app.domain.model.ScanIndicator
import com.aggreyshield.app.domain.model.ScanKind
import com.aggreyshield.app.domain.model.ScanResult
import com.aggreyshield.app.domain.model.Severity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Response shapes of the live AGGREY SHIELD backend, checked against
 * /api/v1/openapi.json on 2026-11-03. URL scans and message scans both return
 * ScanResponse. Nothing is invented here: the confidence word stays text, the
 * score stays the 0-100 rules score, and missing data stays empty.
 */

@Serializable
data class ScanFindingDto(
    val id: String? = null,
    val severity: String? = null,
    val title: String? = null,
    val explanation: String? = null,
    val evidence: String? = null,
)

@Serializable
data class ScanIntelHitDto(
    val source: String? = null,
    @SerialName("indicator_type") val indicatorType: String? = null,
    val category: String? = null,
    val confidence: String? = null,
    val license: String? = null,
)

@Serializable
data class ScanIntelDto(
    val checked: Boolean = false,
    val listed: Boolean = false,
    val hits: List<ScanIntelHitDto> = emptyList(),
)

@Serializable
data class ScanUrlResultDto(
    val url: String? = null,
    val host: String? = null,
    @SerialName("registered_domain") val registeredDomain: String? = null,
)

@Serializable
data class ScanResponseDto(
    val id: String? = null,
    @SerialName("scan_type") val scanType: String? = null,
    @SerialName("risk_score") val riskScore: Int? = null,
    val verdict: String? = null,
    @SerialName("verdict_label") val verdictLabel: String? = null,
    val summary: String? = null,
    val recommendation: String? = null,
    val confidence: String? = null,
    val findings: List<ScanFindingDto> = emptyList(),
    val urls: List<ScanUrlResultDto> = emptyList(),
    val intel: ScanIntelDto? = null,
    val limitations: List<String> = emptyList(),
    @SerialName("input_preview") val inputPreview: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class ScanListItemDto(
    val id: String,
    @SerialName("scan_type") val scanType: String? = null,
    @SerialName("risk_score") val riskScore: Int? = null,
    val verdict: String? = null,
    @SerialName("verdict_label") val verdictLabel: String? = null,
    @SerialName("input_preview") val inputPreview: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class ScanListResponseDto(
    val items: List<ScanListItemDto> = emptyList(),
    val total: Int? = null,
)

/** Visible mapping from the backend verdict to the app severity. Unknown words stay UNKNOWN. */
private fun verdictToSeverity(verdict: String?): Severity = when (verdict?.trim()?.lowercase()) {
    "likely_dangerous" -> Severity.HIGH
    "suspicious" -> Severity.MEDIUM
    "low_risk", "no_warning_signs" -> Severity.LOW
    else -> Severity.UNKNOWN
}

private fun ScanFindingDto.toIndicator(): ScanIndicator {
    val matched = evidence?.takeIf { it.isNotBlank() }?.let { "Matched: $it" }
    val detail = listOfNotNull(explanation, matched).joinToString(" ")
    return ScanIndicator(
        name = title ?: id ?: "Warning sign",
        description = detail.ifBlank { null },
        severity = Severity.fromApi(severity),
    )
}

private fun hitDetail(hit: ScanIntelHitDto): String? =
    listOfNotNull(
        hit.indicatorType,
        hit.confidence?.let { "confidence: $it" },
        hit.license?.let { "licence: $it" },
    ).joinToString(", ").ifBlank { null }

private fun ScanResponseDto.reasonLines(): List<String> {
    val lines = mutableListOf<String>()
    if (!summary.isNullOrBlank()) {
        lines.add(summary)
    }
    val info = intel
    if (info != null && info.listed) {
        info.hits.forEach { hit ->
            val category = hit.category?.let { " ($it)" } ?: ""
            lines.add("Found in a threat list: " + (hit.source ?: "unknown source") + category)
        }
    } else if (info != null && info.checked) {
        lines.add("Not found in the threat lists that were checked. Not being listed means little, because new scams are often not listed yet.")
    }
    return lines
}

fun ScanResponseDto.toScanResult(): ScanResult = ScanResult(
    id = id,
    target = urls.firstOrNull()?.url ?: inputPreview,
    riskLevel = verdictToSeverity(verdict),
    riskScore = riskScore?.toDouble(),
    classification = verdictLabel,
    confidence = null,
    indicators = findings.map { it.toIndicator() },
    reasons = reasonLines(),
    recommendation = recommendation,
    scannedAt = DateTimeUtils.parseInstant(createdAt),
    domainInfo = urls.firstOrNull()
        ?.let { it.registeredDomain ?: it.host }
        ?.let { DomainInfo(domain = it, registrar = null, createdAt = null, ageDays = null, country = null) },
    redirects = emptyList(),
    reputation = intel?.hits.orEmpty().map { ReputationEntry(it.source ?: "Threat list", it.category, hitDetail(it)) },
    modelVersion = null,
    verdictLabel = verdictLabel,
    summary = summary,
    confidenceLabel = confidence,
    limitations = limitations,
)

fun ScanResponseDto.toMessageAnalysis(): MessageAnalysis = MessageAnalysis(
    id = id,
    riskLevel = verdictToSeverity(verdict),
    riskScore = riskScore?.toDouble(),
    classification = verdictLabel,
    confidence = null,
    indicators = findings.map { it.toIndicator() },
    suspiciousPhrases = findings.mapNotNull { f -> f.evidence?.takeIf { it.isNotBlank() } }.distinct(),
    recommendation = recommendation,
    explanation = summary,
    analyzedAt = DateTimeUtils.parseInstant(createdAt),
    modelVersion = null,
    verdictLabel = verdictLabel,
    summary = summary,
    confidenceLabel = confidence,
    limitations = limitations,
)

private fun ScanListItemDto.toHistoryItem(): ScanHistoryItem = ScanHistoryItem(
    id = id,
    kind = ScanKind.fromApi(scanType),
    summary = inputPreview,
    riskLevel = verdictToSeverity(verdict),
    riskScore = riskScore?.toDouble(),
    scannedAt = DateTimeUtils.parseInstant(createdAt),
    verdictLabel = verdictLabel,
)

fun ScanListResponseDto.toDomainPage(): Page<ScanHistoryItem> =
    Page(items.map { it.toHistoryItem() }, total)
