package com.aggreyshield.app.domain.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.domain.model.MessageAnalysis
import com.aggreyshield.app.domain.model.Page
import com.aggreyshield.app.domain.model.ScanHistoryItem
import com.aggreyshield.app.domain.model.ScanResult

/** Every result comes from the backend. Nothing is ever scanned or scored on the device. */
interface ScanRepository {
    suspend fun scanUrl(url: String): ApiResult<ScanResult>
    suspend fun analyzeMessage(content: String): ApiResult<MessageAnalysis>
    suspend fun history(limit: Int): ApiResult<Page<ScanHistoryItem>>
}
