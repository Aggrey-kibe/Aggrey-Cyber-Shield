package com.aggreyshield.app.domain.model

import java.time.Instant

enum class UserRole(val label: String) {
    SUPER_ADMIN("Super administrator"),
    ORGANIZATION_ADMIN("Organization administrator"),
    SECURITY_ANALYST("Security analyst"),
    MANAGER("Manager"),
    EMPLOYEE("Employee"),
    INDIVIDUAL_USER("Individual user"),
    UNKNOWN("Unknown role");

    companion object {
        fun fromApi(value: String?): UserRole =
            entries.firstOrNull { it != UNKNOWN && it.name.equals(value?.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

data class OrganizationSummary(val id: String, val name: String)

data class User(
    val id: String,
    val email: String,
    val fullName: String?,
    val role: UserRole,
    val organization: OrganizationSummary?,
    val emailVerified: Boolean,
    val mfaEnabled: Boolean,
    val createdAt: Instant?,
) {
    val displayName: String get() = fullName?.takeIf { it.isNotBlank() } ?: email
}

data class Organization(
    val id: String,
    val name: String,
    val description: String?,
    val memberCount: Int?,
    val createdAt: Instant?,
)

data class OrganizationMember(
    val userId: String,
    val fullName: String?,
    val email: String?,
    val role: UserRole,
    val joinedAt: Instant?,
)

data class ScoreFactor(val name: String, val description: String?, val impact: Int?)

data class SecurityScore(
    val score: Int,
    val level: Severity,
    val factors: List<ScoreFactor>,
    val computedAt: Instant?,
)

/** Data returned when the server starts MFA enrollment. */
data class MfaSetup(val secret: String, val otpauthUri: String?)
