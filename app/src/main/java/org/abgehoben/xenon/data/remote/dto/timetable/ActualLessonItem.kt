package org.abgehoben.xenon.data.remote.dto.timetable

import kotlinx.serialization.Serializable

@Serializable
data class ActualLessonItem(
    val type: String,
    val date: String? = null,
    val startDate: String? = null,
    val start: String? = null,
    val classHour: ClassHourRef? = null,
    val classHourId: Int? = null,
    val actualLesson: LessonDetail? = null,
    val originalLessons: List<LessonDetail>? = null,
    val event: EventDetail? = null,
    val comment: String? = null
)