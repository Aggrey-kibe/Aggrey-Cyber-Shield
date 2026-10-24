package com.aggreyshield.app.domain.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.domain.model.ServerHealth

interface HealthRepository {
    /** Calls the backend's real GET /api/v1/health endpoint. */
    suspend fun check(): ApiResult<ServerHealth>
}
