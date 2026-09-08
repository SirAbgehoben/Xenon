package org.abgehoben.xenon.data.model.timetable

import org.abgehoben.xenon.data.remote.dto.timetable.ClassHour
import org.abgehoben.xenon.data.remote.dto.timetable.Course
import org.abgehoben.xenon.data.remote.dto.timetable.Room
import org.abgehoben.xenon.data.remote.dto.timetable.Teacher
import org.abgehoben.xenon.data.remote.dto.timetable.TeacherCourseAttendance

data class SchoolMetadata(
    val classHours: List<ClassHour>,
    val courses: List<Course>,
    val rooms: List<Room>,
    val teachers: List<Teacher>,
    val tca: List<TeacherCourseAttendance>
)