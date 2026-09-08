package org.abgehoben.xenon.data.remote.dto.timetable

import kotlinx.serialization.Serializable

@Serializable
data class ClassHour(
    val id: Int,
    val number: Int,
    val from: String,
    val until: String
)