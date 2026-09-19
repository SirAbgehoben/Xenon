package org.abgehoben.xenon.util

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

fun LocalDate.Companion.now(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun LocalTime.Companion.now(): LocalTime =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time

fun LocalDate.plusDays(days: Int): LocalDate = this.plus(days, DateTimeUnit.DAY)
fun LocalDate.plusDays(days: Long): LocalDate = this.plus(days.toInt(), DateTimeUnit.DAY)
fun LocalDate.minusDays(days: Int): LocalDate = this.minus(days, DateTimeUnit.DAY)
fun LocalDate.minusDays(days: Long): LocalDate = this.minus(days.toInt(), DateTimeUnit.DAY)

fun LocalDate.plusWeeks(weeks: Int): LocalDate = this.plus(weeks * 7, DateTimeUnit.DAY)
fun LocalDate.plusWeeks(weeks: Long): LocalDate = this.plus((weeks * 7).toInt(), DateTimeUnit.DAY)
fun LocalDate.minusWeeks(weeks: Int): LocalDate = this.minus(weeks * 7, DateTimeUnit.DAY)
fun LocalDate.minusWeeks(weeks: Long): LocalDate = this.minus((weeks * 7).toInt(), DateTimeUnit.DAY)

fun LocalDate.plusYears(years: Int): LocalDate = this.plus(years, DateTimeUnit.YEAR)
fun LocalDate.plusYears(years: Long): LocalDate = this.plus(years.toInt(), DateTimeUnit.YEAR)
fun LocalDate.minusYears(years: Int): LocalDate = this.minus(years, DateTimeUnit.YEAR)
fun LocalDate.minusYears(years: Long): LocalDate = this.minus(years.toInt(), DateTimeUnit.YEAR)

fun LocalDate.withMonth(month: Int): LocalDate = LocalDate(this.year, month, this.dayOfMonth.coerceAtMost(28))
fun LocalDate.withDayOfMonth(day: Int): LocalDate = LocalDate(this.year, this.monthNumber, day)

val LocalDate.monthValue: Int get() = this.monthNumber

fun LocalDate.isBefore(other: LocalDate): Boolean = this < other
fun LocalDate.isAfter(other: LocalDate): Boolean = this > other
fun LocalTime.isBefore(other: LocalTime): Boolean = this < other
fun LocalTime.isAfter(other: LocalTime): Boolean = this > other

fun LocalDateTime.toLocalDate(): LocalDate = this.date
fun LocalDateTime.toLocalTime(): LocalTime = this.time
fun LocalDate.atStartOfDay(): LocalDateTime = LocalDateTime(this, LocalTime(0, 0))

val DayOfWeek.value: Int get() = this.isoDayNumber

fun LocalTime.toSecondOfDay(): Int = hour * 3600 + minute * 60 + second

object TimeDurationUtils {
    fun between(start: LocalTime, end: LocalTime): Duration {
        val diffSeconds = end.toSecondOfDay() - start.toSecondOfDay()
        return diffSeconds.seconds
    }
}

data class YearMonth(val year: Int, val monthNumber: Int) : Comparable<YearMonth> {
    companion object {
        fun now(): YearMonth {
            val today = LocalDate.now()
            return YearMonth(today.year, today.monthNumber)
        }
    }

    val month: Month get() = Month(monthNumber)

    val lengthOfMonth: Int
        get() = when (monthNumber) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

    fun lengthOfMonth(): Int = lengthOfMonth

    fun atDay(day: Int): LocalDate = LocalDate(year, monthNumber, day)

    fun plusMonths(months: Int): YearMonth = plusMonths(months.toLong())
    fun minusMonths(months: Int): YearMonth = plusMonths(-months.toLong())

    fun plusMonths(months: Long): YearMonth {
        val totalMonths = year * 12 + (monthNumber - 1) + months
        val newYear = (totalMonths / 12).toInt()
        val newMonth = (totalMonths % 12 + 1).toInt()
        return YearMonth(newYear, newMonth)
    }

    fun minusMonths(months: Long): YearMonth = plusMonths(-months)

    override fun compareTo(other: YearMonth): Int =
        compareValuesBy(this, other, { it.year }, { it.monthNumber })

    private fun isLeapYear(y: Int): Boolean = (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0)
}
fun LocalDate.getIsoWeekNumber(): Int {
    val dayOfYear = this.dayOfYear
    val dow = this.dayOfWeek.isoDayNumber
    val weekNumber = (dayOfYear - dow + 10) / 7
    return if (weekNumber < 1) 52 else if (weekNumber > 52) 1 else weekNumber
}