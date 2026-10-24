package com.aggreyshield.app.domain.model

import java.time.Instant

data class AppNotification(
    val id: String,
    val title: String,
    val message: String?,
    val type: String?,
    val read: Boolean,
    val createdAt: Instant?,
)
