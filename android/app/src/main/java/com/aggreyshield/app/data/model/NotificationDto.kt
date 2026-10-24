package com.aggreyshield.app.data.model

import com.aggreyshield.app.core.common.DateTimeUtils
import com.aggreyshield.app.domain.model.AppNotification
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: String,
    val title: String,
    val message: String? = null,
    val type: String? = null,
    val read: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

fun NotificationDto.toDomain(): AppNotification = AppNotification(
    id = id,
    title = title,
    message = message,
    type = type,
    read = read,
    createdAt = DateTimeUtils.parseInstant(createdAt),
)
