package com.aggreyshield.app.data.model

import com.aggreyshield.app.core.common.DateTimeUtils
import com.aggreyshield.app.domain.model.CourseDetail
import com.aggreyshield.app.domain.model.CourseStatus
import com.aggreyshield.app.domain.model.Lesson
import com.aggreyshield.app.domain.model.QuizAnswer
import com.aggreyshield.app.domain.model.QuizOption
import com.aggreyshield.app.domain.model.QuizQuestion
import com.aggreyshield.app.domain.model.QuizResult
import com.aggreyshield.app.domain.model.TrainingCourse
import com.aggreyshield.app.domain.model.TrainingTopic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrainingCourseDto(
    val id: String,
    val title: String,
    val topic: String? = null,
    val description: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("lesson_count") val lessonCount: Int? = null,
    @SerialName("progress_percent") val progressPercent: Int? = null,
    val status: String? = null,
)

@Serializable
data class LessonDto(
    val id: String,
    val title: String,
    val content: String? = null,
    val completed: Boolean = false,
)

@Serializable
data class QuizOptionDto(val id: String, val text: String)

@Serializable
data class QuizQuestionDto(val id: String, val text: String, val options: List<QuizOptionDto> = emptyList())

@Serializable
data class QuizResultDto(
    val id: String? = null,
    @SerialName("course_id") val courseId: String? = null,
    @SerialName("course_title") val courseTitle: String? = null,
    @SerialName("score_percent") val scorePercent: Double? = null,
    val correct: Int? = null,
    val total: Int? = null,
    val passed: Boolean = false,
    @SerialName("completed_at") val completedAt: String? = null,
)

@Serializable
data class CourseDetailDto(
    val course: TrainingCourseDto,
    val lessons: List<LessonDto> = emptyList(),
    val quiz: List<QuizQuestionDto> = emptyList(),
    @SerialName("last_result") val lastResult: QuizResultDto? = null,
)

@Serializable
data class QuizAnswerDto(
    @SerialName("question_id") val questionId: String,
    @SerialName("option_id") val optionId: String,
)

@Serializable
data class SubmitQuizRequestDto(val answers: List<QuizAnswerDto>)

fun TrainingCourseDto.toDomain(): TrainingCourse {
    val progress = (progressPercent ?: 0).coerceIn(0, 100)
    return TrainingCourse(
        id = id,
        title = title,
        topic = TrainingTopic.fromApi(topic),
        description = description,
        durationMinutes = durationMinutes,
        lessonCount = lessonCount,
        progressPercent = progress,
        status = CourseStatus.fromApi(status, progress),
    )
}

fun QuizResultDto.toDomain(): QuizResult = QuizResult(
    id = id,
    courseId = courseId,
    courseTitle = courseTitle,
    scorePercent = (scorePercent ?: 0.0).toInt().coerceIn(0, 100),
    correct = correct,
    total = total,
    passed = passed,
    completedAt = DateTimeUtils.parseInstant(completedAt),
)

fun CourseDetailDto.toDomain(): CourseDetail = CourseDetail(
    course = course.toDomain(),
    lessons = lessons.map { Lesson(it.id, it.title, it.content, it.completed) },
    quiz = quiz.map { q -> QuizQuestion(q.id, q.text, q.options.map { QuizOption(it.id, it.text) }) },
    lastResult = lastResult?.toDomain(),
)

fun QuizAnswer.toDto(): QuizAnswerDto = QuizAnswerDto(questionId, optionId)
