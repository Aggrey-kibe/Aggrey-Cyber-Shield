package com.aggreyshield.app.data.api

import com.aggreyshield.app.core.network.ApiEndpoints
import com.aggreyshield.app.data.model.HealthDto
import retrofit2.http.GET
import retrofit2.http.Header

interface HealthApi {
    @GET(ApiEndpoints.HEALTH)
    suspend fun check(@Header(ApiEndpoints.HEADER_NO_AUTH) noAuth: Boolean = true): HealthDto
}
