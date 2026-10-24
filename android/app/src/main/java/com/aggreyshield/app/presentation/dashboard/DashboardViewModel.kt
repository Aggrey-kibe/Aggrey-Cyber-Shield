package com.aggreyshield.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.Loadable
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.toLoadable
import com.aggreyshield.app.domain.model.Incident
import com.aggreyshield.app.domain.model.ScanHistoryItem
import com.aggreyshield.app.domain.model.SecurityAlert
import com.aggreyshield.app.domain.model.SecurityScore
import com.aggreyshield.app.domain.model.TrainingCourse
import com.aggreyshield.app.domain.model.User
import com.aggreyshield.app.domain.repository.AlertRepository
import com.aggreyshield.app.domain.repository.IncidentRepository
import com.aggreyshield.app.domain.repository.ScanRepository
import com.aggreyshield.app.domain.repository.TrainingRepository
import com.aggreyshield.app.domain.repository.UserRepository
import com.aggreyshield.app.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardData(
    val profile: User,
    val securityScore: SecurityScore,
    val recentAlerts: List<SecurityAlert>,
    val recentIncidents: List<Incident>,
    val recentScans: List<ScanHistoryItem>,
    val trainingCourses: List<TrainingCourse>,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val alertRepository: AlertRepository,
    private val incidentRepository: IncidentRepository,
    private val scanRepository: ScanRepository,
    private val trainingRepository: TrainingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<Loadable<DashboardData>>(Loadable.Loading)
    val uiState: StateFlow<Loadable<DashboardData>> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        _uiState.value = Loadable.Loading
        viewModelScope.launch {
            _uiState.value = fetchDashboard().toLoadable()
        }
    }

    private suspend fun fetchDashboard(): ApiResult<DashboardData> = coroutineScope {
        val profileDeferred = async { userRepository.getProfile() }
        val scoreDeferred = async { userRepository.getSecurityScore() }
        val alertsDeferred = async { alertRepository.list(status = null, limit = 5, offset = 0) }
        val incidentsDeferred = async { incidentRepository.list(status = null, limit = 5, offset = 0) }
        val scansDeferred = async { scanRepository.history(limit = 5) }
        val coursesDeferred = async { trainingRepository.courses() }

        val profile = profileDeferred.await()
        val score = scoreDeferred.await()
        val alerts = alertsDeferred.await()
        val incidents = incidentsDeferred.await()
        val scans = scansDeferred.await()
        val courses = coursesDeferred.await()

        val results = listOf(profile, score, alerts, incidents, scans, courses)
        val firstFailure = results.filterIsInstance<ApiResult.Failure>().firstOrNull()
        if (firstFailure != null) return@coroutineScope firstFailure

        ApiResult.Success(
            DashboardData(
                profile = (profile as ApiResult.Success).data,
                securityScore = (score as ApiResult.Success).data,
                recentAlerts = (alerts as ApiResult.Success).data.items,
                recentIncidents = (incidents as ApiResult.Success).data.items,
                recentScans = (scans as ApiResult.Success).data.items,
                trainingCourses = (courses as ApiResult.Success).data.items,
            ),
        )
    }

    fun onSecurityScoreClicked() = navigate(Routes.SECURITY_SCORE)
    fun onScannerClicked() = navigate(Routes.SCANNER_HUB)
    fun onReportIncidentClicked() = navigate(Routes.INCIDENT_REPORT)
    fun onViewAllIncidentsClicked() = navigate(Routes.INCIDENT_LIST)
    fun onViewAllAlertsClicked() = navigate(Routes.ALERTS)
    fun onTrainingClicked() = navigate(Routes.TRAINING_LIST)
    fun onNotificationsClicked() = navigate(Routes.NOTIFICATIONS)
    fun onIncidentClicked(id: String) = navigate(Routes.incidentDetail(id))

    private fun navigate(route: String) {
        viewModelScope.launch { _events.send(OneShotEvent.Navigate(route)) }
    }
}
