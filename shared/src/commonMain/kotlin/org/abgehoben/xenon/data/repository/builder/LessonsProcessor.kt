package org.abgehoben.xenon.data.repository.builder

import org.abgehoben.xenon.data.model.timetable.LessonStatus
import org.abgehoben.xenon.data.model.timetable.SubstitutionSummary
import org.abgehoben.xenon.data.model.timetable.TimetableSlot
import org.abgehoben.xenon.data.remote.dto.timetable.ActualLessonItem
import org.abgehoben.xenon.data.remote.dto.timetable.ClassHour
import org.abgehoben.xenon.data.repository.util.DateTimeParser
import org.abgehoben.xenon.platform.PlatformDateFormatter
import org.abgehoben.xenon.util.*
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

object LessonsProcessor {

    fun processActualLessons(
        monday: LocalDate,
        friday: LocalDate,
        items: List<ActualLessonItem>,
        classHourMap: Map<Int, ClassHour>,
        grid: MutableMap<Int, MutableMap<Int, TimetableSlot?>>,
        subsSummary: MutableList<SubstitutionSummary>
    ) {
        // Pass 1: Regular, Changed, and Cancelled Lessons
        for (item in items) {
            if (item.type == "event") continue

            val dateStr = item.date ?: item.startDate ?: item.start ?: continue
            val date = DateTimeParser.parseDateFlexible(dateStr) ?: continue
            if (date.isBefore(monday) || date.isAfter(friday)) continue

            val dayIdx = date.dayOfWeek.value
            if (dayIdx !in 1..5) continue

            val hour = extractHourNumber(item, classHourMap) ?: continue
            if (grid[dayIdx]!![hour]?.status == LessonStatus.HOLIDAY) continue

            when (item.type) {
                "regularLesson" -> processRegularLesson(item, grid[dayIdx]!!, hour)
                "changedLesson" -> processChangedLesson(item, grid[dayIdx]!!, hour, date, dayIdx, subsSummary)
                "cancelledLesson" -> processCancelledLesson(item, grid[dayIdx]!!, hour, date, dayIdx, subsSummary)
            }
        }

        // Pass 2: Events and Displacements
        for (item in items) {
            if (item.type != "event") continue

            val dateStr = item.date ?: item.startDate ?: item.start ?: continue
            val date = DateTimeParser.parseDateFlexible(dateStr) ?: continue
            val dayIdx = date.dayOfWeek.value
            if (dayIdx !in 1..5) continue

            val hour = extractHourNumber(item, classHourMap) ?: continue
            if (grid[dayIdx]!![hour]?.status == LessonStatus.HOLIDAY) continue

            processEventLesson(item, grid[dayIdx]!!, hour, date, dayIdx, subsSummary)
        }
    }

    private fun extractHourNumber(item: ActualLessonItem, classHourMap: Map<Int, ClassHour>): Int? {
        item.classHour?.number?.let { return it }
        val chId = item.classHour?.id ?: item.classHourId
        return classHourMap[chId]?.number ?: chId
    }

    private fun processRegularLesson(
        item: ActualLessonItem,
        dayGrid: MutableMap<Int, TimetableSlot?>,
        hour: Int
    ) {
        val lesson = item.actualLesson ?: return
        val subject = lesson.subjectLabel ?: lesson.subject?.abbreviation ?: "Unterricht"
        val room = lesson.room?.name.orEmpty()
        val teachers = lesson.teachers?.mapNotNull { it.abbreviation }?.joinToString(", ").orEmpty()

        dayGrid[hour] = TimetableSlot(
            course = subject,
            teacher = teachers,
            room = room,
            status = LessonStatus.REGULAR,
            courseId = lesson.courseId,
            lessonId = lesson.lessonId
        )
    }

    private fun processChangedLesson(
        item: ActualLessonItem,
        dayGrid: MutableMap<Int, TimetableSlot?>,
        hour: Int,
        date: LocalDate,
        dayIdx: Int,
        subsSummary: MutableList<SubstitutionSummary>
    ) {
        val actual = item.actualLesson ?: return
        val orig = item.originalLessons?.firstOrNull()

        val subject = actual.subjectLabel
            ?: orig?.subjectLabel
            ?: actual.subject?.abbreviation
            ?: orig?.subject?.abbreviation
            ?: "Unterricht"

        val originalRoom = orig?.room?.name.orEmpty()
        val newRoom = actual.room?.name.orEmpty()
        val hasRoomChange = newRoom.isNotEmpty() && originalRoom.isNotEmpty() && newRoom != originalRoom

        val teachers = actual.teachers?.mapNotNull { it.abbreviation }?.joinToString(", ").orEmpty()
        val comment = item.comment ?: actual.comment

        dayGrid[hour] = TimetableSlot(
            course = subject,
            teacher = teachers,
            room = originalRoom.ifEmpty { newRoom },
            status = LessonStatus.SUBSTITUTION,
            substitution = comment,
            newRoom = if (hasRoomChange) newRoom else null,
            subRoom = if (hasRoomChange) newRoom else null,
            courseId = orig?.courseId ?: actual.courseId,
            lessonId = orig?.lessonId ?: actual.lessonId
        )

        subsSummary.add(
            SubstitutionSummary(
                date = DateTimeParser.formatDateGerman(date),
                dayName = PlatformDateFormatter.formatFullDayOfWeek(DayOfWeek(dayIdx)),
                hours = "Stunde $hour",
                cancelled = false,
                text = "$subject${if (hasRoomChange) " (Raum -> $newRoom)" else ""}${if (comment != null) " -> $comment" else ""}"
            )
        )
    }

    private fun processCancelledLesson(
        item: ActualLessonItem,
        dayGrid: MutableMap<Int, TimetableSlot?>,
        hour: Int,
        date: LocalDate,
        dayIdx: Int,
        subsSummary: MutableList<SubstitutionSummary>
    ) {
        val orig = item.originalLessons?.firstOrNull()
        val subject = orig?.subjectLabel ?: orig?.subject?.abbreviation ?: "Unterricht"
        val room = orig?.room?.name.orEmpty()
        val teachers = orig?.teachers?.mapNotNull { it.abbreviation }?.joinToString(", ").orEmpty()

        dayGrid[hour] = TimetableSlot(
            course = subject,
            teacher = teachers,
            room = room,
            status = LessonStatus.CANCELLED,
            courseId = orig?.courseId,
            lessonId = orig?.lessonId
        )

        subsSummary.add(
            SubstitutionSummary(
                date = DateTimeParser.formatDateGerman(date),
                dayName = PlatformDateFormatter.formatFullDayOfWeek(DayOfWeek(dayIdx)),
                hours = "Stunde $hour",
                cancelled = true,
                text = "$subject [ENTFÄLLT]"
            )
        )
    }

    private fun processEventLesson(
        item: ActualLessonItem,
        dayGrid: MutableMap<Int, TimetableSlot?>,
        hour: Int,
        date: LocalDate,
        dayIdx: Int,
        subsSummary: MutableList<SubstitutionSummary>
    ) {
        val event = item.event ?: return
        val title = event.text?.trim()?.takeIf { it.isNotBlank() }
        val rooms = event.rooms?.mapNotNull { it.name }?.joinToString("").orEmpty()
        val teachers = event.teachers?.mapNotNull { it.abbreviation }?.joinToString(" , ").orEmpty()

        val existing = dayGrid[hour]

        if (existing != null) {
            val repText = title ?: if (teachers.isNotEmpty() && teachers != existing.teacher) teachers else null
            val hasRoomChange = rooms.isNotEmpty() && rooms != existing.room

            dayGrid[hour] = existing.copy(
                status = LessonStatus.SUBSTITUTION,
                substitution = repText,
                subRoom = if (hasRoomChange) rooms else (existing.subRoom ?: existing.newRoom),
                teacher = teachers.ifEmpty { existing.teacher }
            )
        } else {
            val displayTitle = title ?: "Veranstaltung"
            dayGrid[hour] = TimetableSlot(
                course = displayTitle,
                teacher = teachers,
                room = rooms,
                status = LessonStatus.SUBSTITUTION,
                substitution = displayTitle,
                newRoom = null,
                subRoom = rooms.ifEmpty { null }
            )
        }

        subsSummary.add(
            SubstitutionSummary(
                date = DateTimeParser.formatDateGerman(date),
                dayName = PlatformDateFormatter.formatFullDayOfWeek(DayOfWeek(dayIdx)),
                hours = "Stunde $hour",
                cancelled = false,
                text = "${existing?.course?.let { "$it -> " } ?: ""}${title ?: teachers}${if (rooms.isNotEmpty()) " ($rooms)" else ""}"
            )
        )
    }
}