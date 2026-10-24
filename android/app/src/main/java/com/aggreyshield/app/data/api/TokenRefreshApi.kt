package com.aggreyshield.app.data.api

import com.aggreyshield.app.core.network.ApiEndpoints
import com.aggreyshield.app.data.model.RefreshRequestDto
import com.aggreyshield.app.data.model.TokenResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * A separate, minimal interface for exactly one call: refreshing tokens.
 * Kept apart from [AuthApi] so [com.aggreyshield.app.core.network.TokenAuthenticator] can call it
 * directly without going through the authenticated OkHttpClient it is itself installed on
 * (which would risk an infinite 401 -> refresh -> 401 loop).
 */
interface TokenRefreshApi {
    @POST(ApiEndpoints.AUTH_REFRESH)
    suspend fun refresh(
        @Body request: RefreshRequestDto,
        @Header(ApiEndpoints.HEADER_NO_AUTH) noAuth: Boolean = true,
    ): TokenResponseDto
}
