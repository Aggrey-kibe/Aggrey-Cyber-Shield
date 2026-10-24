package com.aggreyshield.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body of the backend's real GET /api/v1/health endpoint. */
@Serializable
data class HealthDto(
    val status: String? = null,
    @SerialName("app_name") val appName: String? = null,
    @SerialName("app_version") val appVersion: String? = null,
    val environment: String? = null,
    val database: String? = null,
)
