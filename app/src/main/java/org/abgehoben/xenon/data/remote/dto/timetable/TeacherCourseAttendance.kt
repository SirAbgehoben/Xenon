package org.abgehoben.xenon.data.remote.dto.timetable

import kotlinx.serialization.Serializable

@Serializable
data class TeacherCourseAttendance(
    val courseId: Int,
    val teacherId: Int
)