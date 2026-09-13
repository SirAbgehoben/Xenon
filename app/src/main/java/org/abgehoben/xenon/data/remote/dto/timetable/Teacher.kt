package org.abgehoben.xenon.data.remote.dto.timetable

import kotlinx.serialization.Serializable

@Serializable
data class Teacher(
    val id: Int? = null,
    val abbreviation: String? = null,
    val lastname: String? = null
)