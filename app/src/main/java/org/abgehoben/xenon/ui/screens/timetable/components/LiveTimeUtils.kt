package org.abgehoben.xenon.ui.screens.timetable.components

import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.abgehoben.xenon.data.remote.dto.timetable.ClassHour
import org.abgehoben.xenon.ui.screens.timetable.util.TimetableLayoutUtils
import java.time.Duration
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun rememberLiveTime(): LocalTime {
    var time by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L.milliseconds)
            time = LocalTime.now()
        }
    }
    return time
}

fun calculateCurrentTimeYOffset(
    now: LocalTime,
    totalHours: Int,
    classHours: List<ClassHour>,
    baseHourHeight: Dp,
    scaleBreaks: Boolean
): Dp? {
    val (firstStartStr, _) = TimetableLayoutUtils.getTimeRangeForHour(1, classHours)
    val (_, lastEndStr) = TimetableLayoutUtils.getTimeRangeForHour(totalHours, classHours)

    val dayStart = runCatching { LocalTime.parse(firstStartStr) }.getOrNull() ?: return null
    val dayEnd = runCatching { LocalTime.parse(lastEndStr) }.getOrNull() ?: return null

    if (now.isBefore(dayStart) || now.isAfter(dayEnd)) return null

    var accumulatedY = 0.dp
    for (h in 1..totalHours) {
        val (startStr, endStr) = TimetableLayoutUtils.getTimeRangeForHour(h, classHours)
        val hStart = runCatching { LocalTime.parse(startStr) }.getOrNull() ?: continue
        val hEnd = runCatching { LocalTime.parse(endStr) }.getOrNull() ?: continue

        if (!now.isBefore(hStart) && !now.isAfter(hEnd)) {
            val totalMinutes = Duration.between(hStart, hEnd).toMinutes().coerceAtLeast(1)
            val elapsedMinutes = Duration.between(hStart, now).toMinutes()
            val fraction = elapsedMinutes.toFloat() / totalMinutes
            return accumulatedY + (baseHourHeight * fraction)
        }

        accumulatedY += baseHourHeight

        if (h < totalHours) {
            val breakMin = TimetableLayoutUtils.getBreakMinutesAfter(h, classHours)
            val gapDp = TimetableLayoutUtils.getBreakGapDp(breakMin, scaleBreaks)

            val nextStartStr = TimetableLayoutUtils.getTimeRangeForHour(h + 1, classHours).first
            val nextStart = runCatching { LocalTime.parse(nextStartStr) }.getOrNull()

            if (nextStart != null && now.isAfter(hEnd) && now.isBefore(nextStart)) {
                val breakDuration = Duration.between(hEnd, nextStart).toMinutes().coerceAtLeast(1)
                val elapsedBreak = Duration.between(hEnd, now).toMinutes()
                val fraction = elapsedBreak.toFloat() / breakDuration
                return accumulatedY + (gapDp * fraction)
            }

            accumulatedY += gapDp
        }
    }
    return null
}