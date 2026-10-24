package com.aggreyshield.app.data.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.network.ApiCaller
import com.aggreyshield.app.data.api.ThreatIntelApi
import com.aggreyshield.app.data.model.toDomain
import com.aggreyshield.app.data.model.toPage
import com.aggreyshield.app.domain.model.IndicatorType
import com.aggreyshield.app.domain.model.Page
import com.aggreyshield.app.domain.model.ThreatIndicator
import com.aggreyshield.app.domain.repository.ThreatIntelRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreatIntelRepositoryImpl @Inject constructor(
    private val threatIntelApi: ThreatIntelApi,
    private val apiCaller: ApiCaller,
) : ThreatIntelRepository {

    override suspend fun indicators(
        type: IndicatorType?,
        query: String?,
        limit: Int,
        offset: Int,
    ): ApiResult<Page<ThreatIndicator>> =
        apiCaller.call {
            threatIntelApi.indicators(type?.name, query?.trim()?.takeIf { it.isNotEmpty() }, limit, offset)
                .toPage { it.toDomain() }
        }
}
