package org.abgehoben.xenon.platform

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import java.time.format.TextStyle
import java.util.Locale

actual object PlatformDateFormatter {
    actual fun getShortDayOfWeekNames(): List<String> {
        val locale = Locale.getDefault()
        return listOf(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY,
            java.time.DayOfWeek.SATURDAY,
            java.time.DayOfWeek.SUNDAY
        ).map { it.getDisplayName(TextStyle.SHORT, locale).trimEnd('.') }
    }

    actual fun formatShortDayOfWeek(date: LocalDate): String {
        return formatShortDayOfWeek(date.dayOfWeek)
    }

    actual fun formatShortDayOfWeek(dayOfWeek: DayOfWeek): String {
        val jvmDow = java.time.DayOfWeek.of(dayOfWeek.isoDayNumber)
        return jvmDow.getDisplayName(TextStyle.SHORT, Locale.getDefault()).trimEnd('.')
    }

    actual fun formatFullDayOfWeek(dayOfWeek: DayOfWeek): String {
        val jvmDow = java.time.DayOfWeek.of(dayOfWeek.isoDayNumber)
        return jvmDow.getDisplayName(TextStyle.FULL, Locale.getDefault())
    }

    actual fun formatDayAndMonth(date: LocalDate): String {
        val d = date.dayOfMonth.toString().padStart(2, '0')
        val m = date.monthNumber.toString().padStart(2, '0')
        return "$d.$m."
    }

    actual fun formatMonthAndYear(year: Int, monthNumber: Int): String {
        val month = java.time.Month.of(monthNumber)
        val name = month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        return "$name $year"
    }

    actual fun formatEventDate(date: LocalDate): String {
        val dow = formatShortDayOfWeek(date)
        val d = date.dayOfMonth.toString().padStart(2, '0')
        val m = date.monthNumber.toString().padStart(2, '0')
        val y = (date.year % 100).toString().padStart(2, '0')
        return "$dow | $d.$m.$y"
    }
}