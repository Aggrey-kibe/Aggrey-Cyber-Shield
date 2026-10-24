package com.aggreyshield.app.presentation.training

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.common.Loadable
import com.aggreyshield.app.core.common.OneShotEvent
import com.aggreyshield.app.core.common.toLoadable
import com.aggreyshield.app.domain.model.CourseDetail
import com.aggreyshield.app.domain.model.QuizAnswer
import com.aggreyshield.app.domain.model.QuizResult
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

data class CourseDetailUiState(
    val course: Loadable<CourseDetail> = Loadable.Loading,
    val selectedAnswers: Map<String, String> = emptyMap(),
    val isSubmittingQuiz: Boolean = false,
    val submittedResult: QuizResult? = null,
    val completingLessonId: String? = null,
)

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val courseId: String = Uri.decode(savedStateHandle.get<String>(Routes.ARG_COURSE_ID).orEmpty())

    private val _uiState = MutableStateFlow(CourseDetailUiState())
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events: Flow<OneShotEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onLessonToggle(lessonId: String) {
        _uiState.update { it.copy(completingLessonId = lessonId) }
        viewModelScope.launch {
            val result = trainingRepository.completeLesson(courseId, lessonId)
            _uiState.update { it.copy(completingLessonId = null) }
            when (result) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _events.send(OneShotEvent.ShowMessage(result.error.userMessage))
            }
        }
    }

    fun onAnswerSelected(questionId: String, optionId: String) {
        _uiState.update { it.copy(selectedAnswers = it.selectedAnswers + (questionId to optionId)) }
    }

    fun onSubmitQuiz() {
        val state = _uiState.value
        val quiz = (state.course as? Loadable.Loaded)?.data?.quiz.orEmpty()
        if (quiz.isEmpty()) return

        val unanswered = quiz.any { it.id !in state.selectedAnswers }
        if (unanswered) {
            viewModelScope.launch { _events.send(OneShotEvent.ShowMessage("Answer every question before submitting.")) }
            return
        }

        _uiState.update { it.copy(isSubmittingQuiz = true) }
        viewModelScope.launch {
            val answers = quiz.map { QuizAnswer(it.id, state.selectedAnswers.getValue(it.id)) }
            val result = trainingRepository.submitQuiz(courseId, answers)
            _uiState.update { it.copy(isSubmittingQuiz = false) }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(submittedResult = result.data) }
                    val message = if (result.data.passed) "You passed the quiz!" else "Quiz submitted. Review the material and try again."
                    _events.send(OneShotEvent.ShowMessage(message))
                }
                is ApiResult.Failure -> _events.send(OneShotEvent.ShowMessage(result.error.userMessage))
            }
        }
    }

    private fun load() {
        _uiState.update { it.copy(course = Loadable.Loading) }
        viewModelScope.launch {
            _uiState.update { it.copy(course = trainingRepository.course(courseId).toLoadable()) }
        }
    }
}
