package org.abgehoben.xenon.ui.screens.timetable.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.abgehoben.xenon.data.model.timetable.MergedSlot
import org.abgehoben.xenon.data.model.timetable.TimetableSlot
import org.abgehoben.xenon.data.remote.dto.timetable.ClassHour
import java.time.Duration
import java.time.LocalTime

object TimetableLayoutUtils {

    fun getMergedSlotsForDay(
        daySlots: Map<Int, TimetableSlot?>,
        maxHours: Int = daySlots.keys.maxOrNull()?.coerceAtLeast(1) ?: 1,
        mergeLessons: Boolean = true,
        startHour: Int = 1
    ): List<MergedSlot> {
        if (!mergeLessons) {
            return (startHour..maxHours).map { h -> MergedSlot(h, 1, daySlots[h]) }
        }

        val result = mutableListOf<MergedSlot>()
        var h = startHour
        while (h <= maxHours) {
            val current = daySlots[h]
            if (current == null) {
                var span = 1
                while (h + span <= maxHours && daySlots[h + span] == null) {
                    span++
                }
                result.add(MergedSlot(h, span, null))
                h += span
            } else {
                var span = 1
                while (h + span <= maxHours) {
                    val next = daySlots[h + span]
                    if (next != null && isSameLesson(current, next)) {
                        span++
                    } else {
                        break
                    }
                }
                result.add(MergedSlot(h, span, current))
                h += span
            }
        }
        return result
    }

    fun isSameLesson(a: TimetableSlot, b: TimetableSlot): Boolean {
        if (a.isHoliday && b.isHoliday) return a.course == b.course
        if (a.isHoliday != b.isHoliday) return false
        if (a.cancelled != b.cancelled) return false
        if (a.substitution != b.substitution) return false
        if (a.course != b.course) return false
        if (a.teacher != b.teacher) return false
        val roomA = a.subRoom ?: a.newRoom ?: a.room
        val roomB = b.subRoom ?: b.newRoom ?: b.room
        if (roomA != roomB) return false
        if (a.courseId != null && b.courseId != null && a.courseId != b.courseId) return false
        return true
    }

    fun getTimeRangeForHour(hour: Int, classHours: List<ClassHour>): Pair<String, String> {
        val ch = classHours.find { it.number == hour }
        if (ch != null && ch.from.length >= 5 && ch.until.length >= 5) {
            return Pair(ch.from.substring(0, 5), ch.until.substring(0, 5))
        }
        return Pair("", "")
    }

    fun getBreakMinutesAfter(hour: Int, classHours: List<ClassHour>): Long {
        val ch = classHours.find { it.number == hour }
        val nextCh = classHours.find { it.number == hour + 1 }
        if (ch != null && nextCh != null && ch.until.isNotEmpty() && nextCh.from.isNotEmpty()) {
            try {
                val until = LocalTime.parse(ch.until.take(8))
                val from = LocalTime.parse(nextCh.from.take(8))
                val diff = Duration.between(until, from).toMinutes()
                if (diff >= 0) return diff
            } catch (_: Exception) {}
        }
        return 0L
    }

    /**
     * Resolves the school's standard period duration in minutes.
     */
    fun getStandardPeriodDurationMinutes(classHours: List<ClassHour>): Long {
        for (ch in classHours) {
            val fromStr = ch.from.take(5)
            val untilStr = ch.until.take(5)
            if (fromStr.length >= 5 && untilStr.length >= 5) {
                val start = runCatching { LocalTime.parse(fromStr) }.getOrNull()
                val end = runCatching { LocalTime.parse(untilStr) }.getOrNull()
                if (start != null && end != null) {
                    val diff = Duration.between(start, end).toMinutes()
                    if (diff in 30..90) return diff
                }
            }
        }
        return 45L
    }

    /**
     * Physically accurate break height calculation.
     * 1 minute of break = (baseHourHeight / periodDurationMinutes) dp.
     */
    fun getBreakGapDp(
        breakMinutes: Long,
        scaleBreaks: Boolean = true,
        baseHourHeight: Dp = 50.dp,
        periodDurationMinutes: Long = 45L
    ): Dp {
        val minGap = 4.dp
        if (!scaleBreaks || breakMinutes <= 0) return minGap
        val safeMinutes = periodDurationMinutes.coerceAtLeast(1)
        val proportionalDp = baseHourHeight * (breakMinutes.toFloat() / safeMinutes.toFloat())
        return maxOf(minGap, proportionalDp)
    }
}