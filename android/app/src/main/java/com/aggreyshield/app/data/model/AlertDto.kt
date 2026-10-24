package com.aggreyshield.app.data.model

import com.aggreyshield.app.core.common.DateTimeUtils
import com.aggreyshield.app.domain.model.AlertStatus
import com.aggreyshield.app.domain.model.SecurityAlert
import com.aggreyshield.app.domain.model.Severity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SecurityAlertDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val severity: String? = null,
    val status: String? = null,
    val source: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

fun SecurityAlertDto.toDomain(): SecurityAlert = SecurityAlert(
    id = id,
    title = title,
    description = description,
    severity = Severity.fromApi(severity),
    status = AlertStatus.fromApi(status),
    source = source,
    createdAt = DateTimeUtils.parseInstant(createdAt),
)
