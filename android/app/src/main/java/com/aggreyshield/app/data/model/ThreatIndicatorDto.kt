package com.aggreyshield.app.data.model

import com.aggreyshield.app.core.common.DateTimeUtils
import com.aggreyshield.app.domain.model.IndicatorType
import com.aggreyshield.app.domain.model.Severity
import com.aggreyshield.app.domain.model.ThreatIndicator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ThreatIndicatorDto(
    val id: String,
    val type: String? = null,
    val value: String,
    val severity: String? = null,
    val source: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("first_seen") val firstSeen: String? = null,
    @SerialName("last_seen") val lastSeen: String? = null,
)

fun ThreatIndicatorDto.toDomain(): ThreatIndicator = ThreatIndicator(
    id = id,
    type = IndicatorType.fromApi(type),
    value = value,
    severity = Severity.fromApi(severity),
    source = source,
    description = description,
    tags = tags,
    firstSeen = DateTimeUtils.parseInstant(firstSeen),
    lastSeen = DateTimeUtils.parseInstant(lastSeen),
)
