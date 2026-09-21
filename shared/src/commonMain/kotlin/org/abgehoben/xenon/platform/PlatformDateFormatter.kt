package org.abgehoben.xenon.platform

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

expect object PlatformDateFormatter {
    fun getShortDayOfWeekNames(): List<String>
    fun formatShortDayOfWeek(date: LocalDate): String
    fun formatShortDayOfWeek(dayOfWeek: DayOfWeek): String
    fun formatFullDayOfWeek(dayOfWeek: DayOfWeek): String
    fun formatDayAndMonth(date: LocalDate): String
    fun formatMonthAndYear(year: Int, monthNumber: Int): String
    fun formatEventDate(date: LocalDate): String
}