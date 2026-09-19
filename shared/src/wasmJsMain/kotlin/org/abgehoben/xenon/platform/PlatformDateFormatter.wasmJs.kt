package org.abgehoben.xenon.platform

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number

actual object PlatformDateFormatter {
    private val shortDays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    private val fullDays = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
    private val fullMonths = listOf(
        "Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember"
    )

    actual fun getShortDayOfWeekNames(): List<String> = shortDays
    actual fun formatShortDayOfWeek(date: LocalDate): String = formatShortDayOfWeek(date.dayOfWeek)
    actual fun formatShortDayOfWeek(dayOfWeek: DayOfWeek): String =
        shortDays.getOrElse(dayOfWeek.isoDayNumber - 1) { "Mo" }

    actual fun formatFullDayOfWeek(dayOfWeek: DayOfWeek): String =
        fullDays.getOrElse(dayOfWeek.isoDayNumber - 1) { "Montag" }

    actual fun formatDayAndMonth(date: LocalDate): String {
        val d = date.day.toString().padStart(2, '0')
        val m = date.month.number.toString().padStart(2, '0')
        return "$d.$m."
    }

    actual fun formatMonthAndYear(year: Int, monthNumber: Int): String {
        val mName = fullMonths.getOrElse(monthNumber - 1) { "" }
        return "$mName $year"
    }

    actual fun formatEventDate(date: LocalDate): String {
        val dow = formatShortDayOfWeek(date)
        val d = date.day.toString().padStart(2, '0')
        val m = date.month.number.toString().padStart(2, '0')
        val y = (date.year % 100).toString().padStart(2, '0')
        return "$dow | $d.$m.$y"
    }
}