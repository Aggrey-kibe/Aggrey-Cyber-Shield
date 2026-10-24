package com.aggreyshield.app.domain.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.domain.model.Organization
import com.aggreyshield.app.domain.model.OrganizationMember
import com.aggreyshield.app.domain.model.Page
import com.aggreyshield.app.domain.model.SecurityScore
import com.aggreyshield.app.domain.model.User

interface UserRepository {
    suspend fun getProfile(): ApiResult<User>
    suspend fun updateProfile(fullName: String): ApiResult<User>
    suspend fun getSecurityScore(): ApiResult<SecurityScore>
    suspend fun getOrganization(): ApiResult<Organization>
    suspend fun getOrganizationMembers(): ApiResult<Page<OrganizationMember>>
}
