package org.abgehoben.xenon.data.remote.dto.timetable

import kotlinx.serialization.Serializable

@Serializable
data class LessonDetail(
    val lessonId: Int? = null,
    val courseId: Int? = null,
    val subjectLabel: String? = null,
    val subject: Subject? = null,
    val room: Room? = null,
    val teachers: List<Teacher>? = null,
    val comment: String? = null
)