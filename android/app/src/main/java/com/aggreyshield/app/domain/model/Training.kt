package com.aggreyshield.app.domain.model

import java.time.Instant

enum class TrainingTopic(val label: String) {
    PHISHING("Phishing"),
    PASSWORD_SECURITY("Password security"),
    MFA("Multi-factor authentication"),
    SOCIAL_ENGINEERING("Social engineering"),
    SAFE_BROWSING("Safe browsing"),
    DEVICE_SECURITY("Device security"),
    DATA_PROTECTION("Data protection"),
    INCIDENT_REPORTING("Incident reporting"),
    OTHER("General security");

    companion object {
        fun fromApi(value: String?): TrainingTopic =
            entries.firstOrNull { it.name.equals(value?.trim()?.replace(' ', '_'), ignoreCase = true) } ?: OTHER
    }
}

enum class CourseStatus(val label: String) {
    NOT_STARTED("Not started"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed");

    companion object {
        fun fromApi(value: String?, progressPercent: Int): CourseStatus {
            val parsed = entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) }
            if (parsed != null) return parsed
            return when {
                progressPercent >= 100 -> COMPLETED
                progressPercent > 0 -> IN_PROGRESS
                else -> NOT_STARTED
            }
        }
    }
}

data class TrainingCourse(
    val id: String,
    val title: String,
    val topic: TrainingTopic,
    val description: String?,
    val durationMinutes: Int?,
    val lessonCount: Int?,
    val progressPercent: Int,
    val status: CourseStatus,
)

data class Lesson(val id: String, val title: String, val content: String?, val completed: Boolean)

data class QuizOption(val id: String, val text: String)

data class QuizQuestion(val id: String, val text: String, val options: List<QuizOption>)

data class QuizResult(
    val id: String?,
    val courseId: String?,
    val courseTitle: String?,
    val scorePercent: Int,
    val correct: Int?,
    val total: Int?,
    val passed: Boolean,
    val completedAt: Instant?,
)

data class CourseDetail(
    val course: TrainingCourse,
    val lessons: List<Lesson>,
    val quiz: List<QuizQuestion>,
    val lastResult: QuizResult?,
)

data class QuizAnswer(val questionId: String, val optionId: String)
