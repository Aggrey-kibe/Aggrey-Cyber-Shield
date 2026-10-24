package com.aggreyshield.app.domain.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.domain.model.AlertStatus
import com.aggreyshield.app.domain.model.Page
import com.aggreyshield.app.domain.model.SecurityAlert

interface AlertRepository {
    suspend fun list(status: AlertStatus?, limit: Int, offset: Int): ApiResult<Page<SecurityAlert>>
    suspend fun acknowledge(id: String): ApiResult<Unit>
}
