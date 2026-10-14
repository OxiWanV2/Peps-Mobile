package be.lorenzogallo.pepsmobile.data.model

import java.time.LocalDate

data class NewsItem(
    val type: String,
    val badge: String?,
    val title: String,
    val subtitle: String?,
    val body: String,
    val date: String?,
    val recipients: List<String>,
)

data class PepsNotification(
    val id: Long,
    val title: String,
    val text: String,
    val link: String?,
    val date: String,
    val read: Boolean,
)

data class Course(
    val id: Int,
    val name: String,
    val teachers: List<String>,
    val color: Int,
    val textColor: Int,
) {
    val teacherLabel: String get() = teachers.joinToString(", ")
}

data class Lesson(
    val id: Long,
    val courseId: Int,
    val title: String,
    val start: String,
    val end: String,
    val date: LocalDate,
    val room: String,
    val color: Int,
    val lesson: String,
    val description: String,
    val hasEvaluation: Boolean,
    val hasHomework: Boolean,
)

data class UserProfile(
    val firstName: String,
    val lastName: String,
) {
    val displayName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}
