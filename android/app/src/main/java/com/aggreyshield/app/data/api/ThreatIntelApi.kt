package com.aggreyshield.app.data.api

import com.aggreyshield.app.core.network.ApiEndpoints
import com.aggreyshield.app.data.model.PageDto
import com.aggreyshield.app.data.model.ThreatIndicatorDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ThreatIntelApi {
    @GET(ApiEndpoints.THREAT_INTEL_INDICATORS)
    suspend fun indicators(
        @Query("type") type: String?,
        @Query("query") query: String?,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): PageDto<ThreatIndicatorDto>
}
