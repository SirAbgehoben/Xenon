package org.abgehoben.xenon.data.remote.dto.timetable

import kotlinx.serialization.Serializable

@Serializable
data class Room(
    val id: Int? = null,
    val name: String? = null,
    val abbreviation: String? = null
)