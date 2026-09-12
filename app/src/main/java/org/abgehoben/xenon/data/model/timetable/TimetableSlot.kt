package org.abgehoben.xenon.data.model.timetable

import kotlinx.serialization.Serializable

@Serializable
data class TimetableSlot(
    val course: String,
    val teacher: String,
    val room: String,
    val status: LessonStatus = LessonStatus.REGULAR,
    val substitution: String? = null,
    val newRoom: String? = null,
    val subRoom: String? = null,
    val courseId: Int? = null,
    val lessonId: Int? = null
) {
    // Convenience getters
    val cancelled: Boolean get() = status == LessonStatus.CANCELLED
    val isHoliday: Boolean get() = status == LessonStatus.HOLIDAY
    val isSubstitution: Boolean get() = status == LessonStatus.SUBSTITUTION
}