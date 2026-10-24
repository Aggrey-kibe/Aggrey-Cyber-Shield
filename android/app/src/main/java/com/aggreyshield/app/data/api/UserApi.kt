package com.aggreyshield.app.data.api

import com.aggreyshield.app.core.network.ApiEndpoints
import com.aggreyshield.app.data.model.MemberDto
import com.aggreyshield.app.data.model.OrganizationDto
import com.aggreyshield.app.data.model.PageDto
import com.aggreyshield.app.data.model.SecurityScoreDto
import com.aggreyshield.app.data.model.UpdateProfileRequestDto
import com.aggreyshield.app.data.model.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface UserApi {
    @GET(ApiEndpoints.USERS_ME)
    suspend fun getProfile(): UserDto

    @PATCH(ApiEndpoints.USERS_ME)
    suspend fun updateProfile(@Body request: UpdateProfileRequestDto): UserDto

    @GET(ApiEndpoints.USERS_ME_SECURITY_SCORE)
    suspend fun getSecurityScore(): SecurityScoreDto

    @GET(ApiEndpoints.ORGANIZATION_ME)
    suspend fun getOrganization(): OrganizationDto

    @GET(ApiEndpoints.ORGANIZATION_MEMBERS)
    suspend fun getOrganizationMembers(): PageDto<MemberDto>
}
