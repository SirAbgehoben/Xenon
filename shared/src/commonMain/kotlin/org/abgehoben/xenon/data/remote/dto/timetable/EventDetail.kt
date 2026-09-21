package org.abgehoben.xenon.data.remote.dto.timetable

import kotlinx.serialization.Serializable

@Serializable
data class EventDetail(
    val text: String? = null,
    val rooms: List<Room>? = null,
    val teachers: List<Teacher>? = null
)