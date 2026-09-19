package org.abgehoben.xenon.data.repository.builder

import org.abgehoben.xenon.data.model.timetable.LessonStatus
import org.abgehoben.xenon.data.model.timetable.TimetableSlot
import org.abgehoben.xenon.data.remote.dto.calendar.CalendarResponse
import org.abgehoben.xenon.data.repository.util.DateTimeParser
import kotlinx.datetime.LocalDate
import org.abgehoben.xenon.util.*

object HolidayProcessor {

    fun applyVacationHolidays(
        monday: LocalDate,
        grid: MutableMap<Int, MutableMap<Int, TimetableSlot?>>,
        calendar: CalendarResponse
    ) {
        val allEvents = (calendar.holidays ?: emptyList()) +
                (calendar.nonRecurringEvents ?: emptyList()) +
                (calendar.recurringEvents ?: emptyList())

        allEvents.forEach { event ->
            val summary = (event.summary ?: event.title ?: event.name ?: "").trim()
            val summaryLower = summary.lowercase()

            val isVacation = (event.categoryId == -1 || summaryLower.contains("ferien") || summaryLower.contains("feiertag")) &&
                    !summaryLower.contains("ganztag") &&
                    !summaryLower.contains("studientag")

            if (!isVacation) return@forEach

            val startDt = DateTimeParser.parseIsoLocal(event.start ?: event.startDate) ?: return@forEach
            val endDt = DateTimeParser.parseIsoLocal(event.end ?: event.endDate ?: event.start) ?: startDt

            val startDate = startDt.toLocalDate()
            var endDate = endDt.toLocalDate()

            // Handle midnight exclusive boundary
            if ((event.end?.contains("T00:00") == true || event.end?.contains(" 00:00") == true) && endDate.isAfter(startDate)) {
                endDate = endDate.minusDays(1)
            }

            //TODO: figure out if there is another way to do this, I do not like this since it depends on the language, also: implement i18n here
            val holidayTitle = when {
                summaryLower.contains("letzter ferientag") -> {
                    val descLower = (event.description ?: "").lowercase()
                    when {
                        descLower.contains("sommer") -> "Sommerferien"
                        descLower.contains("herbst") -> "Herbstferien"
                        descLower.contains("weihnacht") -> "Weihnachtsferien"
                        descLower.contains("oster") -> "Osterferien"
                        descLower.contains("pfingst") -> "Pfingstferien"
                        else -> "Ferien"
                    }
                }
                summaryLower.contains("sommer") -> "Sommerferien"
                summaryLower.contains("herbst") -> "Herbstferien"
                summaryLower.contains("weihnacht") -> "Weihnachtsferien"
                summaryLower.contains("oster") -> "Osterferien"
                summaryLower.contains("pfingst") -> "Pfingstferien"
                summary.isNotBlank() -> summary
                else -> "Ferien"
            }

            for (day in 1..5) {
                val dayDate = monday.plusDays(day.toLong() - 1)
                if (!dayDate.isBefore(startDate) && !dayDate.isAfter(endDate)) {
                    val dayMap = grid[day] ?: continue
                    for (hour in dayMap.keys) {
                        if (dayMap[hour] == null || dayMap[hour]?.status == LessonStatus.HOLIDAY) {
                            dayMap[hour] = TimetableSlot(
                                course = holidayTitle,
                                teacher = "",
                                room = "",
                                status = LessonStatus.HOLIDAY
                            )
                        }
                    }
                }
            }
        }
    }
}