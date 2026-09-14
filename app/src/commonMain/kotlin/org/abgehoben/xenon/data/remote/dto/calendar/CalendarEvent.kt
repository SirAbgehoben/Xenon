package org.abgehoben.xenon.data.remote.dto.calendar

import kotlinx.serialization.Serializable

@Serializable
data class CalendarEvent(
    val summary: String? = null,
    val title: String? = null,
    val name: String? = null,
    val start: String? = null,
    val end: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val allDay: Boolean = false,
    val description: String? = null,
    val location: String? = null,
    val organizer: String? = null,
    val categoryId: Int? = null
)