package org.abgehoben.xenon.data.model.calendar

import kotlinx.datetime.LocalDate

data class ProcessedEvent(
    val title: String,
    val description: String,
    val location: String,
    val organizer: String,
    val category: String,
    val allDay: Boolean,
    val isHoliday: Boolean,
    val startTime: String,
    val endTime: String,
    val startDate: LocalDate,
    val endDate: LocalDate
)