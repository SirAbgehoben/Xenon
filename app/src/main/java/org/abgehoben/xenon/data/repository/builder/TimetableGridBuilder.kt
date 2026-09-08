package org.abgehoben.xenon.data.repository.builder

import org.abgehoben.xenon.data.model.timetable.SubstitutionSummary
import org.abgehoben.xenon.data.model.timetable.TimetableGrid
import org.abgehoben.xenon.data.model.timetable.TimetableSlot
import org.abgehoben.xenon.data.remote.dto.calendar.CalendarResponse
import org.abgehoben.xenon.data.remote.dto.timetable.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

object TimetableGridBuilder {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val daysMap = mapOf(
        1 to "Montag", 2 to "Dienstag", 3 to "Mittwoch", 4 to "Donnerstag", 5 to "Freitag"
    )

    fun build(
        monday: LocalDate,
        classHours: List<ClassHour>,
        coursesRaw: List<Course>,
        lessons: List<Lesson>,
        rooms: List<Room>,
        teachers: List<Teacher>,
        tca: List<TeacherCourseAttendance>,
        subsRaw: List<Substitution>,
        calendar: CalendarResponse
    ): TimetableGrid {
        val classHourMap = classHours.associateBy { it.id }
        val roomMap = rooms.associate { it.id to (it.name ?: it.abbreviation ?: "") }
        val teacherMap = teachers.associate { it.id to (it.abbreviation ?: it.lastname ?: "") }
        val courseTeacherMap = tca.associate { it.courseId to (teacherMap[it.teacherId] ?: "") }
        val courseMap = coursesRaw.associateBy { it.id }

        val grid = mutableMapOf<Int, MutableMap<Int, TimetableSlot?>>()
        for (day in 1..5) {
            grid[day] = mutableMapOf()
            for (hour in 1..10) grid[day]!![hour] = null
        }

        val calWeek = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val weekType = if (calWeek % 2 == 0) "W2" else "W1"

        val mondayStr = monday.format(dateFormatter)
        val fridayStr = monday.plusDays(4).format(dateFormatter)

        // Only compute occurrence IDs for lessons valid for this specific week
        val activeLessonsThisWeek = lessons.filter { lesson ->
            (lesson.start == null || lesson.start <= fridayStr) &&
                    (lesson.end == null || lesson.end >= mondayStr)
        }
        val allOccIds = activeLessonsThisWeek.mapNotNull { it.occurrenceId }.distinct().sorted()
        val targetOccId = if (allOccIds.size >= 2) {
            if (weekType == "W1") allOccIds[0] else allOccIds[1]
        } else allOccIds.firstOrNull()

        // 1. Place recurring lessons
        for (day in 1..5) {
            val dayDate = monday.plusDays(day.toLong() - 1)
            val dayStr = dayDate.format(dateFormatter)

            lessons.filter { it.dayOfWeek == day }.forEach { lesson ->
                val ch = classHourMap[lesson.classHourId] ?: return@forEach
                val course = courseMap[lesson.courseId] ?: return@forEach
                val room = roomMap[lesson.roomId ?: -1] ?: ""

                if (lesson.start != null && lesson.start > dayStr) return@forEach
                if (lesson.end != null && lesson.end < dayStr) return@forEach
                if (targetOccId != null && lesson.occurrenceId != null && lesson.occurrenceId != targetOccId) return@forEach

                if (ch.number in 1..10) {
                    grid[day]!![ch.number] = TimetableSlot(
                        course = course.name ?: course.abbreviation ?: "Fach",
                        teacher = courseTeacherMap[course.id] ?: course.teacher?.abbreviation ?: "",
                        room = room,
                        courseId = lesson.courseId,
                        lessonId = lesson.id
                    )
                }
            }
        }

        // 2. Apply holidays over lessons
        applyCalendarEvents(monday, grid, calendar)

        // Sommerferien fallback for KW 36 Monday/Tuesday
        if (mondayStr == "2026-08-31") {
            for (day in listOf(1, 2)) {
                for (h in 1..9) {
                    grid[day]!![h] = TimetableSlot(
                        course = "Sommerferien",
                        teacher = "",
                        room = "",
                        isHoliday = true
                    )
                }
            }
        }

        // 3. Apply substitutions with strict course/lesson matching
        val subsSummary = mutableListOf<SubstitutionSummary>()

        subsRaw.filter { it.date in mondayStr..fridayStr }.forEach { sub ->
            val subDate = LocalDate.parse(sub.date)
            val dayIdx = subDate.dayOfWeek.value
            if (dayIdx !in 1..5) return@forEach

            val ch = classHourMap[sub.classHourId] ?: return@forEach
            val slot = grid[dayIdx]?.get(ch.number) ?: return@forEach

            if (slot.isHoliday) return@forEach

            val effectiveSubCourseId = sub.courseId ?: sub.course?.id
            val subLessonIds = buildSet {
                if (sub.lessonId != null) add(sub.lessonId)
                sub.lessons?.forEach { add(it.id) }
            }

            val matchesSlot = when {
                subLessonIds.isNotEmpty() -> slot.lessonId != null && subLessonIds.contains(slot.lessonId)
                effectiveSubCourseId != null -> slot.courseId != null && slot.courseId == effectiveSubCourseId
                else -> false
            }

            if (!matchesSlot) return@forEach

            val subCourseName = sub.course?.name ?: sub.course?.abbreviation ?: ""
            val isDifferentCourse = subCourseName.isNotEmpty() && subCourseName != slot.course
            val isCancelled = sub.cancelled == true
            val repRoom = sub.room?.name ?: roomMap[sub.roomId ?: -1] ?: ""
            val subTeachers = sub.teachers?.joinToString(", ") { it.abbreviation ?: it.lastname ?: "" } ?: ""

            val hasRoomChange = repRoom.isNotEmpty() && repRoom != slot.room
            val hasTeacherChange = subTeachers.isNotEmpty() && subTeachers != slot.teacher
            val repText = sub.comment?.takeIf { it.isNotEmpty() } ?: if (isDifferentCourse) subCourseName else null

            if (isCancelled) {
                grid[dayIdx]!![ch.number] = slot.copy(
                    cancelled = true,
                    substitution = repText,
                    subRoom = repRoom.ifEmpty { null },
                    teacher = subTeachers.ifEmpty { slot.teacher }
                )
                subsSummary.add(
                    SubstitutionSummary(
                        sub.date,
                        daysMap[dayIdx]!!,
                        "Stunde ${ch.number}",
                        true,
                        "${slot.course} [ENTFÄLLT]${if (repText != null) " -> $repText" else ""}"
                    )
                )
            } else if (hasRoomChange || hasTeacherChange || repText != null) {
                grid[dayIdx]!![ch.number] = slot.copy(
                    newRoom = if (hasRoomChange) repRoom else slot.newRoom,
                    teacher = if (hasTeacherChange) subTeachers else slot.teacher,
                    substitution = repText ?: slot.substitution
                )
                val changeDesc = if (hasRoomChange) "Raumwechsel -> $repRoom" else if (hasTeacherChange) "Vertretung -> $subTeachers" else "Vertretung"
                subsSummary.add(
                    SubstitutionSummary(
                        sub.date,
                        daysMap[dayIdx]!!,
                        "Stunde ${ch.number}",
                        false,
                        "${slot.course}: $changeDesc"
                    )
                )
            }
        }

        // TODO: This still is here just for testing, this will / will need to be replaced.
        //applySpecialOverlays(mondayStr, fridayStr, grid, subsSummary)

        return TimetableGrid(calWeek, weekType, monday, grid, subsSummary, classHours)
    }

    private fun applyCalendarEvents(
        monday: LocalDate,
        grid: MutableMap<Int, MutableMap<Int, TimetableSlot?>>,
        calendar: CalendarResponse
    ) {
        val holidayEvents = (calendar.holidays ?: emptyList()) +
                (calendar.nonRecurringEvents ?: emptyList()) +
                (calendar.recurringEvents ?: emptyList())

        holidayEvents.forEach { event ->
            val summary = (event.summary ?: event.title ?: event.name ?: "").trim()
            val summaryLower = summary.lowercase()

            val isHoliday = event.categoryId == -1 ||
                    summaryLower.contains("ferien") ||
                    summaryLower.contains("feiertag") ||
                    summaryLower.contains("unterrichtsfrei")

            if (isHoliday) {
                if (summaryLower.contains("letzter ferientag")) return@forEach

                val startDt = parseIsoLocal(event.start ?: event.startDate ?: return@forEach) ?: return@forEach
                val endDt = parseIsoLocal(event.end ?: event.endDate ?: event.start ?: "") ?: startDt

                val startDate = startDt.toLocalDate()
                var endDate = endDt.toLocalDate()

                if (event.allDay || (endDt.toLocalTime().hour == 0 && endDt.toLocalTime().minute == 0)) {
                    if (endDate.isAfter(startDate)) {
                        endDate = endDate.minusDays(1)
                    }
                }

                val holidayTitle = when {
                    summaryLower.contains("sommer") -> "Sommerferien"
                    summaryLower.contains("herbst") -> "Herbstferien"
                    summaryLower.contains("weihnacht") -> "Weihnachtsferien"
                    summaryLower.contains("oster") -> "Osterferien"
                    summaryLower.contains("pfingst") -> "Pfingstferien"
                    summary.isNotEmpty() -> summary
                    else -> "Ferien"
                }

                for (day in 1..5) {
                    val dayDate = monday.plusDays(day.toLong() - 1)
                    if (!dayDate.isBefore(startDate) && !dayDate.isAfter(endDate)) {
                        for (hour in 1..10) {
                            grid[day]!![hour] = TimetableSlot(
                                course = holidayTitle,
                                teacher = "",
                                room = "",
                                isHoliday = true
                            )
                        }
                    }
                }
            }
        }
    }

    private fun applySpecialOverlays( //TODO THIS IS TEMPORARY FOR TESTING; THIS NEEDS TO BE REPLACED
        mondayStr: String,
        fridayStr: String,
        grid: MutableMap<Int, MutableMap<Int, TimetableSlot?>>,
        subsSummary: MutableList<SubstitutionSummary>
    ) {
        if (mondayStr <= "2026-09-02" && "2026-09-02" <= fridayStr) {
            listOf(1, 2).forEach { h ->
                grid[3]!![h] = grid[3]!![h]?.copy(cancelled = true, substitution = "SW, BN AULA", subRoom = "AULA") ?: grid[3]!![h]
            }
            listOf(7, 8).forEach { h ->
                grid[3]!![h]?.let { s -> grid[3]!![h] = s.copy(cancelled = true) }
            }
        }

        if (mondayStr <= "2026-09-04" && "2026-09-04" <= fridayStr) {
            listOf(1, 2).forEach { h ->
                grid[5]!![h]?.let { s -> grid[5]!![h] = s.copy(cancelled = true, substitution = "One Note/TEAMS-Einführung", subRoom = "R.KLMensa") }
            }
            listOf(3, 4).forEach { h ->
                val existing = grid[5]!![h]
                grid[5]!![h] = TimetableSlot(
                    course = "One Note/Teams",
                    teacher = "MS, SW, ME, BN",
                    room = "R.KLMensa",
                    substitution = "One Note/TEAMS-Einführung",
                    subRoom = "R.KLMensa",
                    courseId = existing?.courseId,
                    lessonId = existing?.lessonId
                )
            }
            subsSummary.add(SubstitutionSummary("04.09.2026", "Freitag", "Stunde 1-4", false, "One Note/TEAMS-Einführung MS, SW, ME, BN (R.KLMensa)"))
        }
    }

    fun parseIsoLocal(dtStr: String): LocalDateTime? {
        if (dtStr.isEmpty()) return null
        return try {
            LocalDateTime.parse(dtStr.replace("Z", "").substring(0, 19))
        } catch (_: Exception) {
            try {
                LocalDate.parse(dtStr.substring(0, 10)).atStartOfDay()
            } catch (_: Exception) {
                null
            }
        }
    }
}