package com.aggreyshield.app.data.model

import com.aggreyshield.app.core.common.DateTimeUtils
import com.aggreyshield.app.domain.model.DomainInfo
import com.aggreyshield.app.domain.model.MessageAnalysis
import com.aggreyshield.app.domain.model.ReputationEntry
import com.aggreyshield.app.domain.model.ScanHistoryItem
import com.aggreyshield.app.domain.model.ScanIndicator
import com.aggreyshield.app.domain.model.ScanKind
import com.aggreyshield.app.domain.model.ScanResult
import com.aggreyshield.app.domain.model.Severity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScanUrlRequestDto(val url: String)

@Serializable
data class ScanMessageRequestDto(val content: String)

@Serializable
data class IndicatorDto(
    val name: String,
    val description: String? = null,
    val severity: String? = null,
)

@Serializable
data class DomainInfoDto(
    val domain: String? = null,
    val registrar: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("age_days") val ageDays: Int? = null,
    val country: String? = null,
)

@Serializable
data class ReputationDto(
    val provider: String,
    val verdict: String? = null,
    val detail: String? = null,
)

@Serializable
data class ScanResultDto(
    val id: String? = null,
    val target: String? = null,
    @SerialName("risk_level") val riskLevel: String? = null,
    @SerialName("risk_score") val riskScore: Double? = null,
    val classification: String? = null,
    val confidence: Double? = null,
    val indicators: List<IndicatorDto> = emptyList(),
    val reasons: List<String> = emptyList(),
    val recommendation: String? = null,
    @SerialName("scanned_at") val scannedAt: String? = null,
    @SerialName("domain_info") val domainInfo: DomainInfoDto? = null,
    val redirects: List<String> = emptyList(),
    val reputation: List<ReputationDto> = emptyList(),
    @SerialName("model_version") val modelVersion: String? = null,
)

@Serializable
data class MessageAnalysisDto(
    val id: String? = null,
    @SerialName("risk_level") val riskLevel: String? = null,
    @SerialName("risk_score") val riskScore: Double? = null,
    val classification: String? = null,
    val confidence: Double? = null,
    val indicators: List<IndicatorDto> = emptyList(),
    @SerialName("suspicious_phrases") val suspiciousPhrases: List<String> = emptyList(),
    val recommendation: String? = null,
    val explanation: String? = null,
    @SerialName("analyzed_at") val analyzedAt: String? = null,
    @SerialName("model_version") val modelVersion: String? = null,
)

@Serializable
data class ScanHistoryItemDto(
    val id: String,
    val kind: String? = null,
    val summary: String? = null,
    @SerialName("risk_level") val riskLevel: String? = null,
    @SerialName("risk_score") val riskScore: Double? = null,
    @SerialName("scanned_at") val scannedAt: String? = null,
)

private fun IndicatorDto.toDomain(): ScanIndicator =
    ScanIndicator(name = name, description = description, severity = Severity.fromApi(severity))

fun ScanResultDto.toDomain(): ScanResult = ScanResult(
    id = id,
    target = target,
    riskLevel = Severity.fromApi(riskLevel),
    riskScore = riskScore,
    classification = classification,
    confidence = confidence,
    indicators = indicators.map { it.toDomain() },
    reasons = reasons,
    recommendation = recommendation,
    scannedAt = DateTimeUtils.parseInstant(scannedAt),
    domainInfo = domainInfo?.let {
        DomainInfo(
            domain = it.domain,
            registrar = it.registrar,
            createdAt = DateTimeUtils.parseInstant(it.createdAt),
            ageDays = it.ageDays,
            country = it.country,
        )
    },
    redirects = redirects,
    reputation = reputation.map { ReputationEntry(it.provider, it.verdict, it.detail) },
    modelVersion = modelVersion,
)

fun MessageAnalysisDto.toDomain(): MessageAnalysis = MessageAnalysis(
    id = id,
    riskLevel = Severity.fromApi(riskLevel),
    riskScore = riskScore,
    classification = classification,
    confidence = confidence,
    indicators = indicators.map { it.toDomain() },
    suspiciousPhrases = suspiciousPhrases,
    recommendation = recommendation,
    explanation = explanation,
    analyzedAt = DateTimeUtils.parseInstant(analyzedAt),
    modelVersion = modelVersion,
)

fun ScanHistoryItemDto.toDomain(): ScanHistoryItem = ScanHistoryItem(
    id = id,
    kind = ScanKind.fromApi(kind),
    summary = summary,
    riskLevel = Severity.fromApi(riskLevel),
    riskScore = riskScore,
    scannedAt = DateTimeUtils.parseInstant(scannedAt),
)
