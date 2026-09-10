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
        maxHours: Int = 9,
        mergeLessons: Boolean = true
    ): List<MergedSlot> {
        if (!mergeLessons) {
            return (1..maxHours).map { h -> MergedSlot(h, 1, daySlots[h]) }
        }

        val result = mutableListOf<MergedSlot>()
        var h = 1
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
        return when (hour) {
            1 -> "08:20" to "09:05"
            2 -> "09:05" to "09:50"
            3 -> "10:10" to "10:55"
            4 -> "10:55" to "11:40"
            5 -> "12:00" to "12:45"
            6 -> "12:45" to "13:30"
            7 -> "14:15" to "15:00"
            8 -> "15:00" to "15:45"
            9 -> "15:45" to "16:30"
            10 -> "16:30" to "17:15"
            else -> "" to ""
        }
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
        return when (hour) {
            2 -> 20L
            4 -> 20L
            6 -> 45L
            else -> 0L
        }
    }

    fun getBreakGapDp(breakMinutes: Long, scaleBreaks: Boolean = true): Dp {
        if (!scaleBreaks) return 4.dp
        return when {
            breakMinutes >= 35 -> 28.dp
            breakMinutes >= 25 -> 20.dp
            breakMinutes >= 15 -> 16.dp
            breakMinutes >= 5  -> 8.dp
            else               -> 4.dp
        }
    }
}