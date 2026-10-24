package com.aggreyshield.app.data.api

import com.aggreyshield.app.core.network.ApiEndpoints
import com.aggreyshield.app.data.model.CourseDetailDto
import com.aggreyshield.app.data.model.PageDto
import com.aggreyshield.app.data.model.QuizResultDto
import com.aggreyshield.app.data.model.SubmitQuizRequestDto
import com.aggreyshield.app.data.model.TrainingCourseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TrainingApi {
    @GET(ApiEndpoints.TRAINING_COURSES)
    suspend fun courses(): PageDto<TrainingCourseDto>

    @GET(ApiEndpoints.TRAINING_COURSE_BY_ID)
    suspend fun course(@Path("id") id: String): CourseDetailDto

    @POST(ApiEndpoints.TRAINING_LESSON_COMPLETE)
    suspend fun completeLesson(@Path("id") courseId: String, @Path("lessonId") lessonId: String)

    @POST(ApiEndpoints.TRAINING_QUIZ_SUBMIT)
    suspend fun submitQuiz(@Path("id") courseId: String, @Body request: SubmitQuizRequestDto): QuizResultDto

    @GET(ApiEndpoints.TRAINING_RESULTS)
    suspend fun results(): PageDto<QuizResultDto>
}
