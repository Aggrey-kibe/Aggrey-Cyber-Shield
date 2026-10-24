package com.aggreyshield.app.testutil

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.domain.model.MessageAnalysis
import com.aggreyshield.app.domain.model.Page
import com.aggreyshield.app.domain.model.ScanHistoryItem
import com.aggreyshield.app.domain.model.ScanResult
import com.aggreyshield.app.domain.repository.ScanRepository

class FakeScanRepository : ScanRepository {
    var nextUrlResult: ApiResult<ScanResult>? = null
    var nextMessageResult: ApiResult<MessageAnalysis>? = null
    var lastScannedUrl: String? = null
    var scanCallCount: Int = 0

    override suspend fun scanUrl(url: String): ApiResult<ScanResult> {
        lastScannedUrl = url
        scanCallCount++
        return nextUrlResult ?: error("nextUrlResult was not configured for this test")
    }

    override suspend fun analyzeMessage(content: String): ApiResult<MessageAnalysis> =
        nextMessageResult ?: error("nextMessageResult was not configured for this test")

    override suspend fun history(limit: Int): ApiResult<Page<ScanHistoryItem>> =
        ApiResult.Success(Page(emptyList(), 0))
}
