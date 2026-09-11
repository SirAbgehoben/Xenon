package org.abgehoben.xenon.data.repository.builder

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.abgehoben.xenon.data.model.timetable.SubstitutionSummary
import org.abgehoben.xenon.data.model.timetable.TimetableGrid
import org.abgehoben.xenon.data.model.timetable.TimetableSlot
import org.abgehoben.xenon.data.remote.dto.calendar.CalendarResponse
import org.abgehoben.xenon.data.remote.dto.timetable.ClassHour
import java.time.LocalDate
import java.time.temporal.IsoFields

object TimetableGridBuilder {

    fun build(
        monday: LocalDate,
        classHours: List<ClassHour>,
        actualLessonsData: JsonElement?,
        calendar: CalendarResponse
    ): TimetableGrid {
        val classHourMap = classHours.associateBy { it.id }
        val friday = monday.plusDays(4)

        val availableHours = classHours.map { it.number }

        val grid = mutableMapOf<Int, MutableMap<Int, TimetableSlot?>>()
        for (day in 1..5) {
            grid[day] = mutableMapOf()
            for (hour in availableHours) {
                grid[day]!![hour] = null
            }
        }

        val calWeek = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val weekType = if (calWeek % 2 == 0) "W2" else "W1"

        // 1. Mark multi-day official school vacations (e.g. Sommerferien Mon & Tue)
        HolidayProcessor.applyVacationHolidays(monday, grid, calendar)

        // 2. Extract schedule elements array
        val items = when (actualLessonsData) {
            is JsonArray -> actualLessonsData
            is JsonObject -> actualLessonsData["lessons"] as? JsonArray
                ?: actualLessonsData["data"] as? JsonArray
                ?: actualLessonsData["results"] as? JsonArray
            else -> null
        } ?: JsonArray(emptyList())

        val subsSummary = mutableListOf<SubstitutionSummary>()

        // 3. Process regular lessons, room/teacher changes, cancellations, and events
        LessonsProcessor.processActualLessons(
            monday = monday,
            friday = friday,
            items = items,
            classHourMap = classHourMap,
            grid = grid,
            subsSummary = subsSummary
        )

        return TimetableGrid(calWeek, weekType, monday, grid, subsSummary, classHours)
    }
}