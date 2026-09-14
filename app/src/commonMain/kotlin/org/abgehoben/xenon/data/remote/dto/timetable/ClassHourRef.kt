package org.abgehoben.xenon.data.remote.dto.timetable

import kotlinx.serialization.Serializable

@Serializable
data class ClassHourRef(
    val id: Int? = null,
    val number: Int? = null
)