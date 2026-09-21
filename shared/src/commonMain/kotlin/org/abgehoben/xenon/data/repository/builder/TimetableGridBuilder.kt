package org.abgehoben.xenon.data.repository.builder

import org.abgehoben.xenon.data.model.timetable.SubstitutionSummary
import org.abgehoben.xenon.data.model.timetable.TimetableGrid
import org.abgehoben.xenon.data.model.timetable.TimetableSlot
import org.abgehoben.xenon.data.remote.dto.calendar.CalendarResponse
import org.abgehoben.xenon.data.remote.dto.timetable.ActualLessonItem
import org.abgehoben.xenon.data.remote.dto.timetable.ClassHour
import org.abgehoben.xenon.util.getIsoWeekNumber
import org.abgehoben.xenon.util.plusDays
import kotlinx.datetime.LocalDate

object TimetableGridBuilder {

    fun build(
        monday: LocalDate,
        classHours: List<ClassHour>,
        actualLessons: List<ActualLessonItem>,
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

        val calWeek = monday.getIsoWeekNumber()
        val weekType = if (calWeek % 2 == 0) "W2" else "W1"

        // 1. Mark multi-day official school vacations
        HolidayProcessor.applyVacationHolidays(monday, grid, calendar)

        val subsSummary = mutableListOf<SubstitutionSummary>()

        // 2. Process typed lessons, room/teacher changes, cancellations, and events
        LessonsProcessor.processActualLessons(
            monday = monday,
            friday = friday,
            items = actualLessons,
            classHourMap = classHourMap,
            grid = grid,
            subsSummary = subsSummary
        )

        return TimetableGrid(calWeek, weekType, monday, grid, subsSummary, classHours)
    }
}