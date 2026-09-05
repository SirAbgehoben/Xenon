package org.abgehoben.xenon.data

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class TimetableSlot(
    val course: String,
    val teacher: String,
    val room: String,
    val cancelled: Boolean = false,
    val substitution: String? = null,
    val newRoom: String? = null,
    val subRoom: String? = null,
    val isHoliday: Boolean = false,
    val courseId: Int? = null,
    val lessonId: Int? = null
)

data class TimetableGrid(
    val calWeek: Int,
    val weekType: String,
    val mondayDate: LocalDate,
    val grid: Map<Int, Map<Int, TimetableSlot?>>,
    val substitutions: List<SubstitutionSummary>,
    val classHours: List<ClassHour> = emptyList()
)

data class SubstitutionSummary(
    val date: String,
    val dayName: String,
    val hours: String,
    val cancelled: Boolean,
    val text: String
)

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

data class MergedSlot(
    val startHour: Int,
    val span: Int,
    val slot: TimetableSlot?,
    val endHour: Int = startHour + span - 1
)

data class SchoolMetadata(
    val classHours: List<ClassHour>,
    val courses: List<Course>,
    val rooms: List<Room>,
    val teachers: List<Teacher>,
    val tca: List<TeacherCourseAttendance>
)