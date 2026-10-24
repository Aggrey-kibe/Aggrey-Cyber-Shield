package com.aggreyshield.app.data.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.network.ApiCaller
import com.aggreyshield.app.data.api.UserApi
import com.aggreyshield.app.data.model.UpdateProfileRequestDto
import com.aggreyshield.app.data.model.toDomain
import com.aggreyshield.app.data.model.toPage
import com.aggreyshield.app.domain.model.Organization
import com.aggreyshield.app.domain.model.OrganizationMember
import com.aggreyshield.app.domain.model.Page
import com.aggreyshield.app.domain.model.SecurityScore
import com.aggreyshield.app.domain.model.User
import com.aggreyshield.app.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val apiCaller: ApiCaller,
) : UserRepository {

    override suspend fun getProfile(): ApiResult<User> =
        apiCaller.call { userApi.getProfile().toDomain() }

    override suspend fun updateProfile(fullName: String): ApiResult<User> =
        apiCaller.call { userApi.updateProfile(UpdateProfileRequestDto(fullName.trim())).toDomain() }

    override suspend fun getSecurityScore(): ApiResult<SecurityScore> =
        apiCaller.call { userApi.getSecurityScore().toDomain() }

    override suspend fun getOrganization(): ApiResult<Organization> =
        apiCaller.call { userApi.getOrganization().toDomain() }

    override suspend fun getOrganizationMembers(): ApiResult<Page<OrganizationMember>> =
        apiCaller.call { userApi.getOrganizationMembers().toPage { it.toDomain() } }
}
