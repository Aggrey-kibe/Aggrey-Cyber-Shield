package com.aggreyshield.app.domain.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.domain.model.AppNotification
import com.aggreyshield.app.domain.model.Page

interface NotificationRepository {
    suspend fun list(unreadOnly: Boolean, limit: Int, offset: Int): ApiResult<Page<AppNotification>>
    suspend fun markRead(id: String): ApiResult<Unit>
    suspend fun markAllRead(): ApiResult<Unit>
}
