package com.aggreyshield.app.data.model

import com.aggreyshield.app.core.common.DateTimeUtils
import com.aggreyshield.app.domain.model.EvidenceItem
import com.aggreyshield.app.domain.model.Incident
import com.aggreyshield.app.domain.model.IncidentCategory
import com.aggreyshield.app.domain.model.IncidentComment
import com.aggreyshield.app.domain.model.IncidentStatus
import com.aggreyshield.app.domain.model.NewIncident
import com.aggreyshield.app.domain.model.Severity
import com.aggreyshield.app.domain.model.TimelineEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateIncidentRequestDto(
    val title: String,
    val description: String,
    val category: String,
    val severity: String,
)

@Serializable
data class TimelineEntryDto(
    val title: String,
    val detail: String? = null,
    val actor: String? = null,
    val at: String? = null,
)

@Serializable
data class IncidentCommentDto(
    val id: String,
    val author: String? = null,
    val body: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class AddCommentRequestDto(val body: String)

@Serializable
data class EvidenceItemDto(
    val id: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long? = null,
)

@Serializable
data class IncidentDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val severity: String? = null,
    val status: String? = null,
    val reporter: String? = null,
    @SerialName("assigned_analyst") val assignedAnalyst: String? = null,
    val resolution: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val timeline: List<TimelineEntryDto> = emptyList(),
    val comments: List<IncidentCommentDto> = emptyList(),
    val evidence: List<EvidenceItemDto> = emptyList(),
)

fun NewIncident.toRequestDto(): CreateIncidentRequestDto = CreateIncidentRequestDto(
    title = title,
    description = description,
    category = category.name,
    severity = severity.name,
)

fun IncidentCommentDto.toDomain(): IncidentComment =
    IncidentComment(id = id, author = author, body = body, createdAt = DateTimeUtils.parseInstant(createdAt))

fun IncidentDto.toDomain(): Incident = Incident(
    id = id,
    title = title,
    description = description,
    category = IncidentCategory.fromApi(category),
    severity = Severity.fromApi(severity),
    status = IncidentStatus.fromApi(status),
    reporter = reporter,
    assignedAnalyst = assignedAnalyst,
    resolution = resolution,
    createdAt = DateTimeUtils.parseInstant(createdAt),
    updatedAt = DateTimeUtils.parseInstant(updatedAt),
    timeline = timeline.map { TimelineEntry(it.title, it.detail, it.actor, DateTimeUtils.parseInstant(it.at)) },
    comments = comments.map { it.toDomain() },
    evidence = evidence.map { EvidenceItem(it.id, it.fileName, it.contentType, it.sizeBytes) },
)
