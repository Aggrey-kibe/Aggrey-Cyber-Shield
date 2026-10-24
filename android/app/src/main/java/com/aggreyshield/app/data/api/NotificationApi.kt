package com.aggreyshield.app.data.api

import com.aggreyshield.app.core.network.ApiEndpoints
import com.aggreyshield.app.data.model.NotificationDto
import com.aggreyshield.app.data.model.PageDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {
    @GET(ApiEndpoints.NOTIFICATIONS)
    suspend fun list(
        @Query("unread_only") unreadOnly: Boolean,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): PageDto<NotificationDto>

    @POST(ApiEndpoints.NOTIFICATION_READ)
    suspend fun markRead(@Path("id") id: String)

    @POST(ApiEndpoints.NOTIFICATIONS_READ_ALL)
    suspend fun markAllRead()
}
