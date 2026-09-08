package org.abgehoben.xenon.data.remote.dto.timetable

import kotlinx.serialization.Serializable

@Serializable
data class Course(
    val id: Int,
    val name: String? = null,
    val abbreviation: String? = null,
    val subject: Subject? = null,
    val teacher: Teacher? = null,
    val teachers: List<Teacher>? = null
)