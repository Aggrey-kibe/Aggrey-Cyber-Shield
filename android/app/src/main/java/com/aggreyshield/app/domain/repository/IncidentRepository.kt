package com.aggreyshield.app.domain.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.domain.model.Incident
import com.aggreyshield.app.domain.model.IncidentComment
import com.aggreyshield.app.domain.model.IncidentStatus
import com.aggreyshield.app.domain.model.NewIncident
import com.aggreyshield.app.domain.model.Page

interface IncidentRepository {
    suspend fun list(status: IncidentStatus?, limit: Int, offset: Int): ApiResult<Page<Incident>>
    suspend fun get(id: String): ApiResult<Incident>
    suspend fun create(incident: NewIncident): ApiResult<Incident>
    suspend fun addComment(incidentId: String, body: String): ApiResult<IncidentComment>
    suspend fun uploadEvidence(incidentId: String, fileUri: String): ApiResult<Unit>
}
