package org.abgehoben.xenon.ui.screens.timetable

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.abgehoben.xenon.data.ClassHour
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
    val (firstStartStr, _) = getTimeRangeForHour(1, classHours)
    val (_, lastEndStr) = getTimeRangeForHour(totalHours, classHours)

    val dayStart = runCatching { LocalTime.parse(firstStartStr) }.getOrNull() ?: return null
    val dayEnd = runCatching { LocalTime.parse(lastEndStr) }.getOrNull() ?: return null

    // Hide indicator if outside school hours
    if (now.isBefore(dayStart) || now.isAfter(dayEnd)) return null

    var accumulatedY = 0.dp
    for (h in 1..totalHours) {
        val (startStr, endStr) = getTimeRangeForHour(h, classHours)
        val hStart = runCatching { LocalTime.parse(startStr) }.getOrNull() ?: continue
        val hEnd = runCatching { LocalTime.parse(endStr) }.getOrNull() ?: continue

        // Current time is inside lesson period h
        if (!now.isBefore(hStart) && !now.isAfter(hEnd)) {
            val totalMinutes = Duration.between(hStart, hEnd).toMinutes().coerceAtLeast(1)
            val elapsedMinutes = Duration.between(hStart, now).toMinutes()
            val fraction = elapsedMinutes.toFloat() / totalMinutes
            return accumulatedY + (baseHourHeight * fraction)
        }

        accumulatedY += baseHourHeight

        // Check break gap after period h
        if (h < totalHours) {
            val breakMin = getBreakMinutesAfter(h, classHours)
            val gapDp = getBreakGapDp(breakMin, scaleBreaks)

            val nextStartStr = getTimeRangeForHour(h + 1, classHours).first
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

@Composable
fun LiveTimeIndicatorOverlay(
    yOffset: Dp,
    currentDayIndex: Int, // 1 (Mon) .. 5 (Fri)
    colWidth: Dp,
    blockGap: Dp,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    val density = LocalDensity.current
    val totalDays = 5

    val yOffsetPx = with(density) { yOffset.toPx() }
    val colWidthPx = with(density) { colWidth.toPx() }
    val blockGapPx = with(density) { blockGap.toPx() }
    val totalWidthPx = (colWidthPx * totalDays) + (blockGapPx * (totalDays - 1))

    Canvas(
        modifier = modifier
    ) {
        drawLine(
            color = lineColor.copy(alpha = 0.35f),
            start = Offset(0f, yOffsetPx),
            end = Offset(totalWidthPx, yOffsetPx),
            strokeWidth = 1.5.dp.toPx()
        )

        if (currentDayIndex in 1..totalDays) {
            val dayLeft = (colWidthPx + blockGapPx) * (currentDayIndex - 1)
            val dayRight = dayLeft + colWidthPx

            drawLine(
                color = lineColor,
                start = Offset(dayLeft, yOffsetPx),
                end = Offset(dayRight, yOffsetPx),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}