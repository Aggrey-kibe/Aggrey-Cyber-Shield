package com.aggreyshield.app.data.api

import com.aggreyshield.app.core.network.ApiEndpoints
import com.aggreyshield.app.data.model.PageDto
import com.aggreyshield.app.data.model.SecurityAlertDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AlertApi {
    @GET(ApiEndpoints.ALERTS)
    suspend fun list(
        @Query("status") status: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): PageDto<SecurityAlertDto>

    @POST(ApiEndpoints.ALERT_ACKNOWLEDGE)
    suspend fun acknowledge(@Path("id") id: String)
}
