package org.abgehoben.xenon.data.remote.dto.timetable

import kotlinx.serialization.Serializable

@Serializable
data class ClassHour(
    val id: Int,
    val number: Int, //This should maybe become a String. Since it could not just be 1 or 2 but also 0 or 5a
    val from: String,
    val until: String
)