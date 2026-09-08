package org.abgehoben.xenon.data.remote.dto.calendar

import kotlinx.serialization.Serializable

@Serializable
data class CalendarResponse(
    val nonRecurringEvents: List<CalendarEvent>? = null,
    val recurringEvents: List<CalendarEvent>? = null,
    val holidays: List<CalendarEvent>? = null
)