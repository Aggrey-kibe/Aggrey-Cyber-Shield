package com.aggreyshield.app.data.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.network.ApiCaller
import com.aggreyshield.app.data.api.NotificationApi
import com.aggreyshield.app.data.model.toDomain
import com.aggreyshield.app.data.model.toPage
import com.aggreyshield.app.domain.model.AppNotification
import com.aggreyshield.app.domain.model.Page
import com.aggreyshield.app.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationApi: NotificationApi,
    private val apiCaller: ApiCaller,
) : NotificationRepository {

    override suspend fun list(unreadOnly: Boolean, limit: Int, offset: Int): ApiResult<Page<AppNotification>> =
        apiCaller.call { notificationApi.list(unreadOnly, limit, offset).toPage { it.toDomain() } }

    override suspend fun markRead(id: String): ApiResult<Unit> =
        apiCaller.call { notificationApi.markRead(id) }

    override suspend fun markAllRead(): ApiResult<Unit> =
        apiCaller.call { notificationApi.markAllRead() }
}
