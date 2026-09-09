package org.abgehoben.xenon.data.repository.builder

import kotlinx.serialization.json.*
import org.abgehoben.xenon.data.model.timetable.SubstitutionSummary
import org.abgehoben.xenon.data.model.timetable.TimetableSlot
import org.abgehoben.xenon.data.remote.dto.timetable.ClassHour
import org.abgehoben.xenon.data.repository.util.DateTimeParser
import java.time.LocalDate

object LessonsProcessor {

    fun processActualLessons(
        monday: LocalDate,
        friday: LocalDate,
        items: JsonArray,
        classHourMap: Map<Int, ClassHour>,
        grid: MutableMap<Int, MutableMap<Int, TimetableSlot?>>,
        subsSummary: MutableList<SubstitutionSummary>
    ) {
        // Pass 1: Regular, Changed, and Cancelled Lessons
        for (elem in items) {
            val item = elem as? JsonObject ?: continue
            val type = item["type"]?.jsonPrimitive?.contentOrNull ?: continue
            if (type == "event") continue

            val dateStr = item["date"]?.jsonPrimitive?.contentOrNull
                ?: item["startDate"]?.jsonPrimitive?.contentOrNull
                ?: item["start"]?.jsonPrimitive?.contentOrNull ?: continue
            val date = DateTimeParser.parseDateFlexible(dateStr) ?: continue
            if (date.isBefore(monday) || date.isAfter(friday)) continue

            val dayIdx = date.dayOfWeek.value
            if (dayIdx !in 1..5) continue

            val hour = extractHourNumber(item, classHourMap) ?: continue
            if (grid[dayIdx]!![hour]?.isHoliday == true) continue

            when (type) {
                "regularLesson" -> processRegularLesson(item, grid[dayIdx]!!, hour)
                "changedLesson" -> processChangedLesson(item, grid[dayIdx]!!, hour, date, dayIdx, subsSummary)
                "cancelledLesson" -> processCancelledLesson(item, grid[dayIdx]!!, hour, date, dayIdx, subsSummary)
            }
        }

        // Pass 2: Events and Displacements
        for (elem in items) {
            val item = elem as? JsonObject ?: continue
            if (item["type"]?.jsonPrimitive?.contentOrNull != "event") continue

            val dateStr = item["date"]?.jsonPrimitive?.contentOrNull ?: continue
            val date = DateTimeParser.parseDateFlexible(dateStr) ?: continue
            val dayIdx = date.dayOfWeek.value
            if (dayIdx !in 1..5) continue

            val hour = extractHourNumber(item, classHourMap) ?: continue
            if (grid[dayIdx]!![hour]?.isHoliday == true) continue

            processEventLesson(item, grid[dayIdx]!!, hour, date, dayIdx, subsSummary)
        }
    }

    private fun extractHourNumber(item: JsonObject, classHourMap: Map<Int, ClassHour>): Int? {
        val chObj = item["classHour"]?.jsonObject
        val chNum = chObj?.get("number")?.jsonPrimitive?.contentOrNull?.toIntOrNull()
        if (chNum != null) return chNum

        val chId = chObj?.get("id")?.jsonPrimitive?.intOrNull
            ?: item["classHourId"]?.jsonPrimitive?.intOrNull
        return classHourMap[chId]?.number ?: chId
    }

    private fun processRegularLesson(
        item: JsonObject,
        dayGrid: MutableMap<Int, TimetableSlot?>,
        hour: Int
    ) {
        val lesson = item["actualLesson"]?.jsonObject ?: return
        val subject = lesson["subjectLabel"]?.jsonPrimitive?.contentOrNull
            ?: lesson["subject"]?.jsonObject?.get("abbreviation")?.jsonPrimitive?.contentOrNull
            ?: "Unterricht"
        val room = lesson["room"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
        val teachers = lesson["teachers"]?.jsonArray?.mapNotNull {
            it.jsonObject["abbreviation"]?.jsonPrimitive?.contentOrNull
        }?.joinToString(", ") ?: ""

        dayGrid[hour] = TimetableSlot(
            course = subject,
            teacher = teachers,
            room = room,
            courseId = lesson["courseId"]?.jsonPrimitive?.intOrNull,
            lessonId = lesson["lessonId"]?.jsonPrimitive?.intOrNull
        )
    }

    private fun processChangedLesson(
        item: JsonObject,
        dayGrid: MutableMap<Int, TimetableSlot?>,
        hour: Int,
        date: LocalDate,
        dayIdx: Int,
        subsSummary: MutableList<SubstitutionSummary>
    ) {
        val actual = item["actualLesson"]?.jsonObject ?: return
        val orig = item["originalLessons"]?.jsonArray?.firstOrNull()?.jsonObject

        val subject = actual["subjectLabel"]?.jsonPrimitive?.contentOrNull
            ?: orig?.get("subjectLabel")?.jsonPrimitive?.contentOrNull
            ?: actual["subject"]?.jsonObject?.get("abbreviation")?.jsonPrimitive?.contentOrNull
            ?: "Unterricht"

        val originalRoom = orig?.get("room")?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
        val newRoom = actual["room"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
        val hasRoomChange = newRoom.isNotEmpty() && originalRoom.isNotEmpty() && newRoom != originalRoom

        val teachers = actual["teachers"]?.jsonArray?.mapNotNull {
            it.jsonObject["abbreviation"]?.jsonPrimitive?.contentOrNull
        }?.joinToString(", ") ?: ""

        val comment = item["comment"]?.jsonPrimitive?.contentOrNull
            ?: actual["comment"]?.jsonPrimitive?.contentOrNull

        dayGrid[hour] = TimetableSlot(
            course = subject,
            teacher = teachers,
            room = originalRoom.ifEmpty { newRoom },
            cancelled = false,
            substitution = comment,
            newRoom = if (hasRoomChange) newRoom else null,
            subRoom = if (hasRoomChange) newRoom else null,
            courseId = orig?.get("courseId")?.jsonPrimitive?.intOrNull ?: actual["courseId"]?.jsonPrimitive?.intOrNull,
            lessonId = orig?.get("lessonId")?.jsonPrimitive?.intOrNull
        )

        subsSummary.add(
            SubstitutionSummary(
                date = date.format(DateTimeParser.GERMAN_DATE_FORMATTER),
                dayName = DateTimeParser.GERMAN_DAYS_MAP[dayIdx] ?: "",
                hours = "Stunde $hour",
                cancelled = false,
                text = "$subject${if (hasRoomChange) " (Raum -> $newRoom)" else ""}${if (comment != null) " -> $comment" else ""}"
            )
        )
    }

    private fun processCancelledLesson(
        item: JsonObject,
        dayGrid: MutableMap<Int, TimetableSlot?>,
        hour: Int,
        date: LocalDate,
        dayIdx: Int,
        subsSummary: MutableList<SubstitutionSummary>
    ) {
        val orig = item["originalLessons"]?.jsonArray?.firstOrNull()?.jsonObject
        val subject = orig?.get("subjectLabel")?.jsonPrimitive?.contentOrNull
            ?: orig?.get("subject")?.jsonObject?.get("abbreviation")?.jsonPrimitive?.contentOrNull
            ?: "Unterricht"
        val room = orig?.get("room")?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull ?: ""
        val teachers = orig?.get("teachers")?.jsonArray?.mapNotNull {
            (it as? JsonObject)?.get("abbreviation")?.jsonPrimitive?.contentOrNull
        }?.joinToString(", ") ?: ""

        dayGrid[hour] = TimetableSlot(
            course = subject,
            teacher = teachers,
            room = room,
            cancelled = true,
            courseId = orig?.get("courseId")?.jsonPrimitive?.intOrNull,
            lessonId = orig?.get("lessonId")?.jsonPrimitive?.intOrNull
        )

        subsSummary.add(
            SubstitutionSummary(
                date = date.format(DateTimeParser.GERMAN_DATE_FORMATTER),
                dayName = DateTimeParser.GERMAN_DAYS_MAP[dayIdx] ?: "",
                hours = "Stunde $hour",
                cancelled = true,
                text = "$subject [ENTFÄLLT]"
            )
        )
    }

    private fun processEventLesson(
        item: JsonObject,
        dayGrid: MutableMap<Int, TimetableSlot?>,
        hour: Int,
        date: LocalDate,
        dayIdx: Int,
        subsSummary: MutableList<SubstitutionSummary>
    ) {
        val event = item["event"]?.jsonObject ?: return
        val title = event["text"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
        val rooms = event["rooms"]?.jsonArray?.mapNotNull {
            it.jsonObject["name"]?.jsonPrimitive?.contentOrNull
        }?.joinToString("") ?: ""
        val teachers = event["teachers"]?.jsonArray?.mapNotNull {
            it.jsonObject["abbreviation"]?.jsonPrimitive?.contentOrNull
        }?.joinToString(" , ") ?: ""

        val existing = dayGrid[hour]

        if (existing != null) {
            val repText = title ?: if (teachers.isNotEmpty() && teachers != existing.teacher) teachers else null
            val hasRoomChange = rooms.isNotEmpty() && rooms != existing.room

            dayGrid[hour] = existing.copy(
                cancelled = true,
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
                cancelled = false,
                substitution = displayTitle,
                newRoom = null,
                subRoom = rooms.ifEmpty { null }
            )
        }

        subsSummary.add(
            SubstitutionSummary(
                date = date.format(DateTimeParser.GERMAN_DATE_FORMATTER),
                dayName = DateTimeParser.GERMAN_DAYS_MAP[dayIdx] ?: "",
                hours = "Stunde $hour",
                cancelled = false,
                text = "${existing?.course?.let { "$it -> " } ?: ""}${title ?: teachers}${if (rooms.isNotEmpty()) " ($rooms)" else ""}"
            )
        )
    }
}