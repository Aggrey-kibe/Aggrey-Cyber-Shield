package com.aggreyshield.app.data.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.core.network.ApiCaller
import com.aggreyshield.app.data.api.TrainingApi
import com.aggreyshield.app.data.model.SubmitQuizRequestDto
import com.aggreyshield.app.data.model.toDomain
import com.aggreyshield.app.data.model.toDto
import com.aggreyshield.app.data.model.toPage
import com.aggreyshield.app.domain.model.CourseDetail
import com.aggreyshield.app.domain.model.Page
import com.aggreyshield.app.domain.model.QuizAnswer
import com.aggreyshield.app.domain.model.QuizResult
import com.aggreyshield.app.domain.model.TrainingCourse
import com.aggreyshield.app.domain.repository.TrainingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainingRepositoryImpl @Inject constructor(
    private val trainingApi: TrainingApi,
    private val apiCaller: ApiCaller,
) : TrainingRepository {

    override suspend fun courses(): ApiResult<Page<TrainingCourse>> =
        apiCaller.call { trainingApi.courses().toPage { it.toDomain() } }

    override suspend fun course(id: String): ApiResult<CourseDetail> =
        apiCaller.call { trainingApi.course(id).toDomain() }

    override suspend fun completeLesson(courseId: String, lessonId: String): ApiResult<Unit> =
        apiCaller.call { trainingApi.completeLesson(courseId, lessonId) }

    override suspend fun submitQuiz(courseId: String, answers: List<QuizAnswer>): ApiResult<QuizResult> =
        apiCaller.call {
            trainingApi.submitQuiz(courseId, SubmitQuizRequestDto(answers.map { it.toDto() })).toDomain()
        }

    override suspend fun results(): ApiResult<Page<QuizResult>> =
        apiCaller.call { trainingApi.results().toPage { it.toDomain() } }
}
