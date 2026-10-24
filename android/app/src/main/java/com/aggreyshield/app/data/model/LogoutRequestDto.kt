package com.aggreyshield.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LogoutRequestDto(
    @SerialName("refresh_token")
    val refreshToken: String,
)