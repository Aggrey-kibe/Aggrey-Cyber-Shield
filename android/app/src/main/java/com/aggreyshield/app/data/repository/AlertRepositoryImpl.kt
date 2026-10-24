package com.aggreyshield.app.data.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.network.ApiCaller
import com.aggreyshield.app.data.api.AlertApi
import com.aggreyshield.app.data.model.toDomain
import com.aggreyshield.app.data.model.toPage
import com.aggreyshield.app.domain.model.AlertStatus
import com.aggreyshield.app.domain.model.Page
import com.aggreyshield.app.domain.model.SecurityAlert
import com.aggreyshield.app.domain.repository.AlertRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val alertApi: AlertApi,
    private val apiCaller: ApiCaller,
) : AlertRepository {

    override suspend fun list(status: AlertStatus?, limit: Int, offset: Int): ApiResult<Page<SecurityAlert>> =
        apiCaller.call { alertApi.list(status?.name, limit, offset).toPage { it.toDomain() } }

    override suspend fun acknowledge(id: String): ApiResult<Unit> =
        apiCaller.call { alertApi.acknowledge(id) }
}
