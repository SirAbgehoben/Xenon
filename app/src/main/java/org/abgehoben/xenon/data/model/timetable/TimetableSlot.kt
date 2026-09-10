package org.abgehoben.xenon.data.model.timetable

import kotlinx.serialization.Serializable

@Serializable
data class TimetableSlot(
    val course: String,
    val teacher: String,
    val room: String,
    val cancelled: Boolean = false,
    val substitution: String? = null,
    val newRoom: String? = null,
    val subRoom: String? = null,
    val isHoliday: Boolean = false,
    val courseId: Int? = null,
    val lessonId: Int? = null
)