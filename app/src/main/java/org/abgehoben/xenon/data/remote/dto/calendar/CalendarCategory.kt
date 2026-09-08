package org.abgehoben.xenon.data.remote.dto.calendar

import kotlinx.serialization.Serializable

@Serializable
data class CalendarCategory(
    val id: Int,
    val name: String
)