package com.aggreyshield.app.data.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.network.ApiCaller
import com.aggreyshield.app.data.api.IncidentApi
import com.aggreyshield.app.data.model.AddCommentRequestDto
import com.aggreyshield.app.data.model.toDomain
import com.aggreyshield.app.data.model.toPage
import com.aggreyshield.app.data.model.toRequestDto
import com.aggreyshield.app.domain.model.Incident
import com.aggreyshield.app.domain.model.IncidentComment
import com.aggreyshield.app.domain.model.IncidentStatus
import com.aggreyshield.app.domain.model.NewIncident
import com.aggreyshield.app.domain.model.Page
import com.aggreyshield.app.domain.repository.EvidenceSource
import com.aggreyshield.app.domain.repository.IncidentRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentRepositoryImpl @Inject constructor(
    private val incidentApi: IncidentApi,
    private val apiCaller: ApiCaller,
    private val evidenceSource: EvidenceSource,
) : IncidentRepository {

    override suspend fun list(status: IncidentStatus?, limit: Int, offset: Int): ApiResult<Page<Incident>> =
        apiCaller.call { incidentApi.list(status?.name, limit, offset).toPage { it.toDomain() } }

    override suspend fun get(id: String): ApiResult<Incident> =
        apiCaller.call { incidentApi.get(id).toDomain() }

    override suspend fun create(incident: NewIncident): ApiResult<Incident> =
        apiCaller.call { incidentApi.create(incident.toRequestDto()).toDomain() }

    override suspend fun addComment(incidentId: String, body: String): ApiResult<IncidentComment> =
        apiCaller.call { incidentApi.addComment(incidentId, AddCommentRequestDto(body.trim())).toDomain() }

    override suspend fun uploadEvidence(incidentId: String, fileUri: String): ApiResult<Unit> =
        apiCaller.call {
            val info = evidenceSource.describe(fileUri) ?: error("Could not read the selected file.")
            val bytes = evidenceSource.read(fileUri)
            val body = bytes.toRequestBody(info.mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", info.name, body)
            incidentApi.uploadEvidence(incidentId, part)
        }
}
