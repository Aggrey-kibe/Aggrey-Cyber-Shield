package com.aggreyshield.app.data.api

import com.aggreyshield.app.core.network.ApiEndpoints
import com.aggreyshield.app.data.model.AddCommentRequestDto
import com.aggreyshield.app.data.model.CreateIncidentRequestDto
import com.aggreyshield.app.data.model.IncidentCommentDto
import com.aggreyshield.app.data.model.IncidentDto
import com.aggreyshield.app.data.model.PageDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface IncidentApi {
    @GET(ApiEndpoints.INCIDENTS)
    suspend fun list(
        @Query("status") status: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): PageDto<IncidentDto>

    @GET(ApiEndpoints.INCIDENT_BY_ID)
    suspend fun get(@Path("id") id: String): IncidentDto

    @POST(ApiEndpoints.INCIDENTS)
    suspend fun create(@Body request: CreateIncidentRequestDto): IncidentDto

    @POST(ApiEndpoints.INCIDENT_COMMENTS)
    suspend fun addComment(@Path("id") id: String, @Body request: AddCommentRequestDto): IncidentCommentDto

    @Multipart
    @POST(ApiEndpoints.INCIDENT_EVIDENCE)
    suspend fun uploadEvidence(@Path("id") id: String, @retrofit2.http.Part file: MultipartBody.Part)
}
