package org.abgehoben.xenon.data.remote.dto.timetable

import kotlinx.serialization.Serializable

@Serializable
data class Substitution(
    val id: Int,
    val date: String,
    val lessonId: Int? = null,
    val courseId: Int? = null,
    val roomId: Int? = null,
    val classHourId: Int? = null,
    val cancelled: Boolean? = null,
    val comment: String? = null,
    val teachers: List<Teacher>? = null,
    val course: Course? = null,
    val room: Room? = null,
    val lessons: List<Lesson>? = null
)