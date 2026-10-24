package com.aggreyshield.app.presentation.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.Loadable
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.map
import com.aggreyshield.app.core.common.toLoadable
import com.aggreyshield.app.domain.model.TrainingCourse
import com.aggreyshield.app.domain.repository.TrainingRepository
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

@HiltViewModel
class TrainingListViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<Loadable<List<TrainingCourse>>>(Loadable.Loading)
    val uiState: StateFlow<Loadable<List<TrainingCourse>>> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onCourseClicked(id: String) {
        viewModelScope.launch { _events.send(OneShotEvent.Navigate(Routes.courseDetail(id))) }
    }

    fun onViewResultsClicked() {
        viewModelScope.launch { _events.send(OneShotEvent.Navigate(Routes.TRAINING_RESULTS)) }
    }

    private fun load() {
        _uiState.value = Loadable.Loading
        viewModelScope.launch {
            _uiState.value = trainingRepository.courses().map { it.items }.toLoadable()
        }
    }
}
