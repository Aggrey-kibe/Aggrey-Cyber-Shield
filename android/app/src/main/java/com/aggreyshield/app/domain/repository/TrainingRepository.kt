package com.aggreyshield.app.domain.repository

import com.aggreyshield.app.core.common.ApiResult
import com.aggreyshield.app.domain.model.CourseDetail
import com.aggreyshield.app.domain.model.Page
import com.aggreyshield.app.domain.model.QuizAnswer
import com.aggreyshield.app.domain.model.QuizResult
import com.aggreyshield.app.domain.model.TrainingCourse

interface TrainingRepository {
    suspend fun courses(): ApiResult<Page<TrainingCourse>>
    suspend fun course(id: String): ApiResult<CourseDetail>
    suspend fun completeLesson(courseId: String, lessonId: String): ApiResult<Unit>
    suspend fun submitQuiz(courseId: String, answers: List<QuizAnswer>): ApiResult<QuizResult>
    suspend fun results(): ApiResult<Page<QuizResult>>
}
