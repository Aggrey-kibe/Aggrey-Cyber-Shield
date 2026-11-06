package com.aggreyshield.app.data.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.network.ApiCaller
import com.aggreyshield.app.data.api.ScanApi
import com.aggreyshield.app.data.model.ScanMessageRequestDto
import com.aggreyshield.app.data.model.ScanUrlRequestDto
import com.aggreyshield.app.data.model.toDomainPage
import com.aggreyshield.app.data.model.toMessageAnalysis
import com.aggreyshield.app.data.model.toScanResult
import com.aggreyshield.app.domain.model.MessageAnalysis
import com.aggreyshield.app.domain.model.Page
import com.aggreyshield.app.domain.model.ScanHistoryItem
import com.aggreyshield.app.domain.model.ScanResult
import com.aggreyshield.app.domain.repository.ScanRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepositoryImpl @Inject constructor(
    private val scanApi: ScanApi,
    private val apiCaller: ApiCaller,
) : ScanRepository {

    override suspend fun scanUrl(url: String): ApiResult<ScanResult> =
        apiCaller.call { scanApi.scanUrl(ScanUrlRequestDto(url)).toScanResult() }

    override suspend fun analyzeMessage(content: String): ApiResult<MessageAnalysis> =
        apiCaller.call { scanApi.analyzeMessage(ScanMessageRequestDto(content)).toMessageAnalysis() }

    override suspend fun history(limit: Int): ApiResult<Page<ScanHistoryItem>> =
        apiCaller.call { scanApi.history(limit).toDomainPage() }
}
