package com.aggreyshield.app.domain.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.domain.model.IndicatorType
import com.aggreyshield.app.domain.model.Page
import com.aggreyshield.app.domain.model.ThreatIndicator

interface ThreatIntelRepository {
    suspend fun indicators(type: IndicatorType?, query: String?, limit: Int, offset: Int): ApiResult<Page<ThreatIndicator>>
}
