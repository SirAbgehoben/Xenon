package org.abgehoben.xenon.data.remote.dto.timetable

import kotlinx.serialization.Serializable

@Serializable
data class Lesson(
    val id: Int,
    val dayOfWeek: Int,
    val classHourId: Int,
    val courseId: Int,
    val roomId: Int? = null,
    val occurrenceId: Int? = null,
    val start: String? = null,
    val end: String? = null
)