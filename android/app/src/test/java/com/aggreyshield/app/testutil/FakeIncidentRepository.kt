package com.aggreyshield.app.testutil

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.domain.model.Incident
import com.aggreyshield.app.domain.model.IncidentCategory
import com.aggreyshield.app.domain.model.IncidentComment
import com.aggreyshield.app.domain.model.IncidentStatus
import com.aggreyshield.app.domain.model.NewIncident
import com.aggreyshield.app.domain.model.Page
import com.aggreyshield.app.domain.model.Severity
import com.aggreyshield.app.domain.repository.IncidentRepository

class FakeIncidentRepository : IncidentRepository {
    var nextCreateResult: ApiResult<Incident>? = null
    var lastCreated: NewIncident? = null
    var createCallCount: Int = 0
    var uploadEvidenceCallCount: Int = 0
    var nextUploadResult: ApiResult<Unit> = ApiResult.Success(Unit)

    override suspend fun list(status: IncidentStatus?, limit: Int, offset: Int): ApiResult<Page<Incident>> =
        ApiResult.Success(Page(emptyList(), 0))

    override suspend fun get(id: String): ApiResult<Incident> =
        nextCreateResult ?: error("not configured")

    override suspend fun create(incident: NewIncident): ApiResult<Incident> {
        lastCreated = incident
        createCallCount++
        return nextCreateResult ?: error("nextCreateResult was not configured for this test")
    }

    override suspend fun addComment(incidentId: String, body: String): ApiResult<IncidentComment> =
        ApiResult.Success(IncidentComment(id = "c1", author = "Tester", body = body, createdAt = null))

    override suspend fun uploadEvidence(incidentId: String, fileUri: String): ApiResult<Unit> {
        uploadEvidenceCallCount++
        return nextUploadResult
    }
}

fun sampleIncident(
    id: String = "incident-1",
    category: IncidentCategory = IncidentCategory.PHISHING,
    severity: Severity = Severity.MEDIUM,
): Incident = Incident(
    id = id,
    title = "Sample incident",
    description = "Sample description",
    category = category,
    severity = severity,
    status = IncidentStatus.OPEN,
    reporter = "tester@example.com",
    assignedAnalyst = null,
    resolution = null,
    createdAt = null,
    updatedAt = null,
    timeline = emptyList(),
    comments = emptyList(),
    evidence = emptyList(),
)
