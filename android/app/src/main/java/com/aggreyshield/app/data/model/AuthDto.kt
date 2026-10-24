package com.aggreyshield.app.data.model

import com.aggreyshield.app.domain.model.MfaSetup
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    @SerialName("full_name") val fullName: String,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
    @SerialName("mfa_code") val mfaCode: String? = null,
)

@Serializable
data class TokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
)

@Serializable
data class RefreshRequestDto(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class VerifyEmailRequestDto(val email: String, val code: String)

@Serializable
data class EmailRequestDto(val email: String)

@Serializable
data class ResetPasswordRequestDto(
    val token: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class ChangePasswordRequestDto(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class CodeRequestDto(val code: String)

@Serializable
data class MfaSetupDto(
    val secret: String,
    @SerialName("otpauth_uri") val otpauthUri: String? = null,
)

fun MfaSetupDto.toDomain(): MfaSetup = MfaSetup(secret = secret, otpauthUri = otpauthUri)
