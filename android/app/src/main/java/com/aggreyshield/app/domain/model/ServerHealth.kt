package com.aggreyshield.app.domain.model

/** Result of the connection test against the backend's real GET /api/v1/health endpoint. */
data class ServerHealth(
    val serverUrl: String,
    val apiReachable: Boolean,
    val databaseConnected: Boolean,
    val environment: String?,
    val appVersion: String?,
)
