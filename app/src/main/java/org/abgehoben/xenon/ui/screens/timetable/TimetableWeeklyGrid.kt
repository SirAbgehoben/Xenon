package org.abgehoben.xenon.ui.screens.timetable

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.abgehoben.xenon.data.model.timetable.MergedSlot
import org.abgehoben.xenon.data.model.timetable.TimetableGrid
import org.abgehoben.xenon.data.model.timetable.TimetableSlot
import org.abgehoben.xenon.ui.screens.timetable.components.LiveTimeIndicatorOverlay
import org.abgehoben.xenon.ui.screens.timetable.components.TimetableDayHeaderRow
import org.abgehoben.xenon.ui.screens.timetable.components.TimetableGridCell
import org.abgehoben.xenon.ui.screens.timetable.components.TimetablePeriodColumn
import org.abgehoben.xenon.ui.screens.timetable.components.calculateCurrentTimeYOffset
import org.abgehoben.xenon.ui.screens.timetable.components.rememberLiveTime
import org.abgehoben.xenon.ui.screens.timetable.util.TimetableLayoutUtils
import org.abgehoben.xenon.ui.theme.Dimens
import java.time.LocalDate

@Composable
fun TimetableWeeklyGrid(
    grid: TimetableGrid,
    mergeLessons: Boolean = true,
    scaleBreaks: Boolean = true,
    onSlotClick: (Int, MergedSlot, TimetableSlot) -> Unit,
    onDayClick: (Int) -> Unit = {}
) {
    val hScrollState = rememberScrollState()
    val vScrollState = rememberScrollState()

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

    val standardPeriodMinutes = remember(classHours) {
        TimetableLayoutUtils.getStandardPeriodDurationMinutes(classHours)
    }

    val breakMinutesList = remember(totalHours, minHour, classHours) {
        (minHour until totalHours).map { h ->
            TimetableLayoutUtils.getBreakMinutesAfter(h, classHours)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TimetableDayHeaderRow(
            mondayDate = grid.mondayDate,
            scrollState = hScrollState,
            onDayClick = onDayClick
        )

        HorizontalDivider(
            thickness = Dimens.StrokeThin,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = Dimens.TimetableBlockGap)
        ) {
            val minGap = 4.dp
            val minHourHeight = 38.dp
            // Deduct a 1.dp buffer to absorb subpixel rounding errors
            val availableHeight = maxHeight - 1.dp

            val (baseHourHeight) = remember(
                availableHeight,
                hoursCount,
                breakMinutesList,
                scaleBreaks,
                standardPeriodMinutes
            ) {
                if (!scaleBreaks) {
                    val totalFixedGaps = minGap * breakMinutesList.size
                    val calculated = (availableHeight - totalFixedGaps) / hoursCount
                    if (calculated >= minHourHeight) calculated to false else minHourHeight to true
                } else {
                    var fixedGapsCount = 0
                    var scaledRatioSum = 0f
                    for (b in breakMinutesList) {
                        if (b <= 0) {
                            fixedGapsCount++
                        } else {
                            scaledRatioSum += b.toFloat() / standardPeriodMinutes.toFloat()
                        }
                    }
                    val totalFixedGaps = minGap * fixedGapsCount
                    val totalScaleUnits = hoursCount.toFloat() + scaledRatioSum
                    val calculated = if (totalScaleUnits > 0f) {
                        (availableHeight - totalFixedGaps) / totalScaleUnits
                    } else minHourHeight

                    if (calculated >= minHourHeight) calculated to false else minHourHeight to true
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                        modifier = Modifier.width(Dimens.TimetableTimeColWidth)
                    )

                    Spacer(modifier = Modifier.width(Dimens.TimetableBlockGap))

                    Box(modifier = Modifier.horizontalScroll(hScrollState)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.TimetableBlockGap)) {
                            for (d in 1..5) {
                                val daySlots = grid.grid[d] ?: emptyMap()
                                val mergedSlots = TimetableLayoutUtils.getMergedSlotsForDay(
                                    daySlots = daySlots,
                                    maxHours = totalHours,
                                    mergeLessons = mergeLessons,
                                    startHour = minHour
                                )

                                Column(modifier = Modifier.width(Dimens.TimetableColWidth)) {
                                    for (merged in mergedSlots) {
                                        var blockHeight = baseHourHeight * merged.span
                                        for (i in merged.startHour until (merged.startHour + merged.span - 1)) {
                                            val breakMin = TimetableLayoutUtils.getBreakMinutesAfter(i, classHours)
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
                                                    if (merged.slot != null) onSlotClick(d, merged, merged.slot)
                                                }
                                        ) {
                                            if (merged.slot != null) {
                                                TimetableGridCell(merged.slot, merged.span)
                                            }
                                        }

                                        if (merged.endHour < totalHours) {
                                            val breakMin = TimetableLayoutUtils.getBreakMinutesAfter(merged.endHour, classHours)
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
                                colWidth = Dimens.TimetableColWidth,
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