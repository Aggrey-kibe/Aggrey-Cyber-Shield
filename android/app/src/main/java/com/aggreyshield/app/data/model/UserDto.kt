package com.aggreyshield.app.data.model

import com.aggreyshield.app.core.common.DateTimeUtils
import com.aggreyshield.app.domain.model.Organization
import com.aggreyshield.app.domain.model.OrganizationMember
import com.aggreyshield.app.domain.model.OrganizationSummary
import com.aggreyshield.app.domain.model.ScoreFactor
import com.aggreyshield.app.domain.model.SecurityScore
import com.aggreyshield.app.domain.model.Severity
import com.aggreyshield.app.domain.model.User
import com.aggreyshield.app.domain.model.UserRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrganizationSummaryDto(val id: String, val name: String = "")

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    @SerialName("full_name") val fullName: String? = null,
    val role: String? = null,
    val organization: OrganizationSummaryDto? = null,
    @SerialName("email_verified") val emailVerified: Boolean = false,
    @SerialName("mfa_enabled") val mfaEnabled: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class UpdateProfileRequestDto(@SerialName("full_name") val fullName: String)

@Serializable
data class OrganizationDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("member_count") val memberCount: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class MemberDto(
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String? = null,
    val email: String? = null,
    val role: String? = null,
    @SerialName("joined_at") val joinedAt: String? = null,
)

@Serializable
data class ScoreFactorDto(
    val name: String,
    val description: String? = null,
    val impact: Double? = null,
)

@Serializable
data class SecurityScoreDto(
    val score: Double,
    val level: String? = null,
    val factors: List<ScoreFactorDto> = emptyList(),
    @SerialName("computed_at") val computedAt: String? = null,
)

fun UserDto.toDomain(): User = User(
    id = id,
    email = email,
    fullName = fullName,
    role = UserRole.fromApi(role),
    organization = organization?.let { OrganizationSummary(it.id, it.name) },
    emailVerified = emailVerified,
    mfaEnabled = mfaEnabled,
    createdAt = DateTimeUtils.parseInstant(createdAt),
)

fun OrganizationDto.toDomain(): Organization = Organization(
    id = id,
    name = name,
    description = description,
    memberCount = memberCount,
    createdAt = DateTimeUtils.parseInstant(createdAt),
)

fun MemberDto.toDomain(): OrganizationMember = OrganizationMember(
    userId = userId,
    fullName = fullName,
    email = email,
    role = UserRole.fromApi(role),
    joinedAt = DateTimeUtils.parseInstant(joinedAt),
)

fun SecurityScoreDto.toDomain(): SecurityScore = SecurityScore(
    score = score.toInt().coerceIn(0, 100),
    level = Severity.fromApi(level),
    factors = factors.map { ScoreFactor(it.name, it.description, it.impact?.toInt()) },
    computedAt = DateTimeUtils.parseInstant(computedAt),
)
