package org.abgehoben.xenon.ui.screens.timetable.views

import androidx.compose.animation.core.animate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.abgehoben.xenon.data.model.timetable.MergedSlot
import org.abgehoben.xenon.data.model.timetable.TimetableGrid
import org.abgehoben.xenon.data.model.timetable.TimetableSlot
import org.abgehoben.xenon.ui.screens.timetable.components.*
import org.abgehoben.xenon.ui.screens.timetable.util.TimetableLayoutUtils
import org.abgehoben.xenon.ui.theme.Dimens
import org.abgehoben.xenon.util.*

@Composable
fun TimetableWeeklyGrid(
    grid: TimetableGrid,
    savedColWidth: Float? = null,
    onColWidthChange: (Float) -> Unit = {},
    mergeLessons: Boolean = true,
    scaleBreaks: Boolean = true,
    onSlotClick: (Int, MergedSlot, TimetableSlot) -> Unit,
    onDayClick: (Int) -> Unit = {}
) {
    val hScrollState = rememberScrollState()
    val vScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val classHours = grid.classHours
    val minHour = minOf(1, classHours.minOfOrNull { it.number } ?: 1)
    val maxClassHour = classHours.maxOfOrNull { it.number } ?: 0
    val maxLessonHour = grid.grid.values.flatMap { dayMap ->
        dayMap.filterValues { slot -> slot != null }.keys
    }.maxOrNull() ?: 0
    val totalHours = maxOf(maxClassHour, maxLessonHour).takeIf { it > 0 } ?: 9
    val hoursCount = totalHours - minHour + 1

    val nowTime = rememberLiveTime()
    val today = LocalDate.now()
    val isCurrentWeek = !today.isBefore(grid.mondayDate) && !today.isAfter(grid.mondayDate.plusDays(4))
    val currentDayIndex = today.dayOfWeek.value
    val dynamicPrimary = MaterialTheme.colorScheme.primary

    // 1. PRE-CALCULATE static timetable data once so the pinch loop does ZERO allocations
    val standardPeriodMinutes = remember(classHours) {
        TimetableLayoutUtils.getStandardPeriodDurationMinutes(classHours)
    }

    val breakMinutesMap = remember(totalHours, minHour, classHours) {
        (minHour..totalHours).associateWith { h ->
            TimetableLayoutUtils.getBreakMinutesAfter(h, classHours)
        }
    }

    val dayMergedSlots = remember(grid, mergeLessons, minHour, totalHours) {
        (1..5).associateWith { d ->
            val daySlots = grid.grid[d] ?: emptyMap()
            TimetableLayoutUtils.getMergedSlotsForDay(
                daySlots = daySlots,
                maxHours = totalHours,
                mergeLessons = mergeLessons,
                startHour = minHour
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableWidth = maxWidth
        val availableHeight = maxHeight - 48.dp

        val totalFixedSpacing = Dimens.TimetableTimeColWidth + (Dimens.TimetableBlockGap * 7)
        val fitColWidth = maxOf(30.dp, (availableWidth - totalFixedSpacing) / 5)

        val minColWidth = fitColWidth
        val defaultColWidth = maxOf(fitColWidth, Dimens.TimetableColWidth)
        val maxColWidth = maxOf(fitColWidth * 1.8f, 180.dp)

        var colWidthDp by rememberSaveable {
            mutableFloatStateOf(savedColWidth ?: defaultColWidth.value)
        }

        LaunchedEffect(savedColWidth) {
            if (savedColWidth != null) {
                colWidthDp = savedColWidth
            }
        }

        val currentColWidth = colWidthDp.dp.coerceIn(minColWidth, maxColWidth)

        val baseHourHeight = remember(
            availableHeight,
            hoursCount,
            breakMinutesMap,
            scaleBreaks,
            standardPeriodMinutes
        ) {
            val minGap = 4.dp
            val minHourHeight = 38.dp
            val breakCount = (minHour until totalHours).count()

            if (!scaleBreaks) {
                val totalFixedGaps = minGap * breakCount
                val calculated = (availableHeight - totalFixedGaps) / hoursCount
                if (calculated >= minHourHeight) calculated else minHourHeight
            } else {
                var fixedGapsCount = 0
                var scaledRatioSum = 0f
                for (h in minHour until totalHours) {
                    val b = breakMinutesMap[h] ?: 0L
                    if (b <= 0) fixedGapsCount++
                    else scaledRatioSum += b.toFloat() / standardPeriodMinutes.toFloat()
                }
                val totalFixedGaps = minGap * fixedGapsCount
                val totalScaleUnits = hoursCount.toFloat() + scaledRatioSum
                val calculated = if (totalScaleUnits > 0f) {
                    (availableHeight - totalFixedGaps) / totalScaleUnits
                } else minHourHeight
                if (calculated >= minHourHeight) calculated else minHourHeight
            }
        }

        val liveYOffset = remember(nowTime, baseHourHeight, scaleBreaks) {
            calculateCurrentTimeYOffset(
                now = nowTime,
                totalHours = totalHours,
                classHours = classHours,
                baseHourHeight = baseHourHeight,
                scaleBreaks = scaleBreaks,
                startHour = minHour,
                periodDurationMinutes = standardPeriodMinutes
            )
        }

        val toggleFitScreen: () -> Unit = {
            scope.launch {
                val target = if (currentColWidth <= fitColWidth + 4.dp) {
                    maxOf(Dimens.TimetableColWidth, fitColWidth * 1.35f).value
                } else {
                    fitColWidth.value
                }
                animate(
                    initialValue = currentColWidth.value,
                    targetValue = target
                ) { value, _ -> colWidthDp = value }

                // Save when toggle animation completes
                onColWidthChange(target)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(minColWidth, maxColWidth) {
                    awaitEachGesture {
                        var prevDistance = 0f
                        var isZooming = false
                        var didZoom = false
                        var lastPointers: List<PointerId> = emptyList()

                        do {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val activePointers = event.changes.filter { it.pressed }

                            if (activePointers.size >= 2) {
                                val p1 = activePointers[0].position
                                val p2 = activePointers[1].position
                                val currentDistance = (p1 - p2).getDistance()
                                val currentPointers = listOf(activePointers[0].id, activePointers[1].id)

                                if (!isZooming || currentPointers != lastPointers) {
                                    isZooming = true
                                    lastPointers = currentPointers
                                    prevDistance = currentDistance
                                } else if (prevDistance > 0f && currentDistance > 0f) {
                                    val zoomFactor = currentDistance / prevDistance
                                    colWidthDp = (colWidthDp * zoomFactor).coerceIn(minColWidth.value, maxColWidth.value)
                                    prevDistance = currentDistance
                                    didZoom = true
                                }
                                event.changes.forEach { it.consume() }
                            } else {
                                isZooming = false
                                prevDistance = 0f
                                lastPointers = emptyList()
                            }
                        } while (event.changes.any { it.pressed })

                        if (didZoom) {
                            onColWidthChange(colWidthDp.coerceIn(minColWidth.value, maxColWidth.value))
                        }
                    }
                }
        ) {
            TimetableDayHeaderRow(
                mondayDate = grid.mondayDate,
                scrollState = hScrollState,
                colWidth = currentColWidth,
                onDayClick = onDayClick,
                onCornerClick = toggleFitScreen
            )

            HorizontalDivider(
                thickness = Dimens.StrokeThin,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = Dimens.TimetableBlockGap)
                    .verticalScroll(vScrollState)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TimetablePeriodColumn(
                        startHour = minHour,
                        totalHours = totalHours,
                        classHours = classHours,
                        baseHourHeight = baseHourHeight,
                        scaleBreaks = scaleBreaks,
                        periodDurationMinutes = standardPeriodMinutes,
                        modifier = Modifier
                            .width(Dimens.TimetableTimeColWidth)
                            .graphicsLayer()
                    )

                    Spacer(modifier = Modifier.width(Dimens.TimetableBlockGap))

                    Box(modifier = Modifier.horizontalScroll(hScrollState)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.TimetableBlockGap)) {
                            for (d in 1..5) {
                                val mergedSlots = dayMergedSlots[d] ?: emptyList()

                                Column(
                                    modifier = Modifier
                                        .width(currentColWidth)
                                        .graphicsLayer()
                                ) {
                                    for (merged in mergedSlots) {
                                        var blockHeight = baseHourHeight * merged.span
                                        for (i in merged.startHour until (merged.startHour + merged.span - 1)) {
                                            val breakMin = breakMinutesMap[i] ?: 0L
                                            blockHeight += TimetableLayoutUtils.getBreakGapDp(
                                                breakMinutes = breakMin,
                                                scaleBreaks = scaleBreaks,
                                                baseHourHeight = baseHourHeight,
                                                periodDurationMinutes = standardPeriodMinutes
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .height(blockHeight)
                                                .fillMaxWidth()
                                                .clickable(enabled = merged.slot != null) {
                                                    if (merged.slot != null) {
                                                        onSlotClick(d, merged, merged.slot)
                                                    }
                                                }
                                        ) {
                                            if (merged.slot != null) {
                                                TimetableGridCell(
                                                    slot = merged.slot,
                                                    span = merged.span,
                                                    isCompact = currentColWidth < 80.dp
                                                )
                                            }
                                        }

                                        if (merged.endHour < totalHours) {
                                            val breakMin = breakMinutesMap[merged.endHour] ?: 0L
                                            val breakGap = TimetableLayoutUtils.getBreakGapDp(
                                                breakMinutes = breakMin,
                                                scaleBreaks = scaleBreaks,
                                                baseHourHeight = baseHourHeight,
                                                periodDurationMinutes = standardPeriodMinutes
                                            )
                                            Spacer(modifier = Modifier.height(breakGap))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(Dimens.TimetableBlockGap))
                        }

                        if (isCurrentWeek && liveYOffset != null) {
                            LiveTimeIndicatorOverlay(
                                yOffset = liveYOffset,
                                currentDayIndex = currentDayIndex,
                                colWidth = currentColWidth,
                                blockGap = Dimens.TimetableBlockGap,
                                lineColor = dynamicPrimary,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    }
                }
            }
        }
    }
}