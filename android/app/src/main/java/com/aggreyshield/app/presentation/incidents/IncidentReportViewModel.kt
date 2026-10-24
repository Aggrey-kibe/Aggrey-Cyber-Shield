package com.aggreyshield.app.presentation.incidents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.InputValidator
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.domain.model.EvidenceFileInfo
import com.aggreyshield.app.domain.model.EvidenceRules
import com.aggreyshield.app.domain.model.IncidentCategory
import com.aggreyshield.app.domain.model.NewIncident
import com.aggreyshield.app.domain.model.Severity
import com.aggreyshield.app.domain.repository.EvidenceSource
import com.aggreyshield.app.domain.repository.IncidentRepository
import com.aggreyshield.app.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IncidentReportUiState(
    val title: String = "",
    val description: String = "",
    val category: IncidentCategory = IncidentCategory.PHISHING,
    val severity: Severity = Severity.MEDIUM,
    val titleError: String? = null,
    val descriptionError: String? = null,
    val evidence: EvidenceFileInfo? = null,
    val evidenceError: String? = null,
    val isSubmitting: Boolean = false,
)

@HiltViewModel
class IncidentReportViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val evidenceSource: EvidenceSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncidentReportUiState())
    val uiState: StateFlow<IncidentReportUiState> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    fun onTitleChanged(value: String) = _uiState.update { it.copy(title = value, titleError = null) }
    fun onDescriptionChanged(value: String) = _uiState.update { it.copy(description = value, descriptionError = null) }
    fun onCategorySelected(value: IncidentCategory) = _uiState.update { it.copy(category = value) }
    fun onSeveritySelected(value: Severity) = _uiState.update { it.copy(severity = value) }

    fun onEvidenceSelected(uri: String?) {
        if (uri == null) {
            _uiState.update { it.copy(evidence = null, evidenceError = null) }
            return
        }
        val info = evidenceSource.describe(uri)
        if (info == null) {
            _uiState.update { it.copy(evidence = null, evidenceError = "Could not read the selected file.") }
            return
        }
        val error = EvidenceRules.validate(info)
        _uiState.update { it.copy(evidence = if (error == null) info else null, evidenceError = error) }
    }

    fun onRemoveEvidence() = _uiState.update { it.copy(evidence = null, evidenceError = null) }

    fun onSubmit() {
        val state = _uiState.value
        val titleError = InputValidator.validateIncidentTitle(state.title)
        val descriptionError = InputValidator.validateIncidentDescription(state.description)
        if (titleError != null || descriptionError != null) {
            _uiState.update { it.copy(titleError = titleError, descriptionError = descriptionError) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val created = incidentRepository.create(
                NewIncident(
                    title = state.title.trim(),
                    description = state.description.trim(),
                    category = state.category,
                    severity = state.severity,
                ),
            )
            when (created) {
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.send(OneShotEvent.ShowMessage(created.error.userMessage))
                }
                is ApiResult.Success -> {
                    val incidentId = created.data.id
                    var message = "Incident reported successfully."

                    val evidence = state.evidence
                    if (evidence != null) {
                        val uploadResult = incidentRepository.uploadEvidence(incidentId, evidence.uri)
                        if (uploadResult is ApiResult.Failure) {
                            message = "Incident reported, but the evidence file could not be uploaded: ${uploadResult.error.userMessage}"
                        }
                    }

                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.send(OneShotEvent.ShowMessage(message))
                    _events.send(
                        OneShotEvent.Navigate(
                            Routes.incidentDetail(incidentId),
                            popUpToRoute = Routes.INCIDENT_LIST,
                            inclusive = false,
                        ),
                    )
                }
            }
        }
    }
}
