package com.aggreyshield.app.data.api

import com.aggreyshield.app.core.network.ApiEndpoints
import com.aggreyshield.app.data.model.ScanListResponseDto
import com.aggreyshield.app.data.model.ScanResponseDto
import com.aggreyshield.app.data.model.ScanMessageRequestDto
import com.aggreyshield.app.data.model.ScanUrlRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ScanApi {
    @POST(ApiEndpoints.SCAN_URL)
    suspend fun scanUrl(@Body request: ScanUrlRequestDto): ScanResponseDto

    @POST(ApiEndpoints.SCAN_MESSAGE)
    suspend fun analyzeMessage(@Body request: ScanMessageRequestDto): ScanResponseDto

    @GET(ApiEndpoints.SCAN_HISTORY)
    suspend fun history(@Query("limit") limit: Int): ScanListResponseDto
}
