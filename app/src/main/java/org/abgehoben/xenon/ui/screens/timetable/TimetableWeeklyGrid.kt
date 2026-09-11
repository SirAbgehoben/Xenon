package org.abgehoben.xenon.ui.screens.timetable

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abgehoben.xenon.R
import org.abgehoben.xenon.data.model.timetable.MergedSlot
import org.abgehoben.xenon.data.model.timetable.TimetableGrid
import org.abgehoben.xenon.data.model.timetable.TimetableSlot
import org.abgehoben.xenon.ui.screens.timetable.components.LiveTimeIndicatorOverlay
import org.abgehoben.xenon.ui.screens.timetable.components.TimetablePeriodColumn
import org.abgehoben.xenon.ui.screens.timetable.components.calculateCurrentTimeYOffset
import org.abgehoben.xenon.ui.screens.timetable.components.rememberLiveTime
import org.abgehoben.xenon.ui.screens.timetable.util.TimetableLayoutUtils
import org.abgehoben.xenon.ui.theme.Dimens
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun UntisWeeklyGrid(
    grid: TimetableGrid,
    mergeLessons: Boolean = true,
    scaleBreaks: Boolean = true,
    onSlotClick: (Int, MergedSlot, TimetableSlot) -> Unit
) {
    val hScrollState = rememberScrollState()
    val vScrollState = rememberScrollState()

    val currentLocale = LocalConfiguration.current.locales[0]
    val dFormatter = remember(currentLocale) {
        DateTimeFormatter.ofPattern("dd.MM.", currentLocale)
    }

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
    val dynamicPrimaryContainer = MaterialTheme.colorScheme.primaryContainer
    val dynamicOnPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    val standardPeriodMinutes = remember(classHours) {
        TimetableLayoutUtils.getStandardPeriodDurationMinutes(classHours)
    }

    val breakMinutesList = remember(totalHours, minHour, classHours) {
        (minHour until totalHours).map { h ->
            TimetableLayoutUtils.getBreakMinutesAfter(h, classHours)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = Dimens.ElevationLevel1
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.TimetableBlockGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.width(Dimens.TimetableTimeColWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.period_abbr),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(Dimens.TimetableBlockGap))

                Row(
                    modifier = Modifier.horizontalScroll(hScrollState),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.TimetableBlockGap)
                ) {
                    for (i in 0..4) {
                        val date = grid.mondayDate.plusDays(i.toLong())
                        val isToday = date == today
                        val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, currentLocale)

                        Box(
                            modifier = Modifier.width(Dimens.TimetableColWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(Dimens.RadiusMedium),
                                color = if (isToday) dynamicPrimaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isToday) BorderStroke(Dimens.StrokeMedium, dynamicPrimary) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = dayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isToday) dynamicOnPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = date.format(dFormatter),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = if (isToday) dynamicPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(Dimens.TimetableBlockGap))
                }
            }
        }

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
            val minHourHeight = 38.dp // Readable threshold for landscape/tiny screens

            // Solve for exact baseHourHeight so: (hours * baseHeight + breaks) == maxHeight
            val (baseHourHeight, isScrollable) = remember(
                maxHeight,
                hoursCount,
                breakMinutesList,
                scaleBreaks,
                standardPeriodMinutes
            ) {
                if (!scaleBreaks) {
                    val totalFixedGaps = minGap * breakMinutesList.size
                    val calculated = (maxHeight - totalFixedGaps) / hoursCount
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
                        (maxHeight - totalFixedGaps) / totalScaleUnits
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

            val scrollModifier = if (isScrollable) Modifier.verticalScroll(vScrollState) else Modifier

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(scrollModifier)
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
                                                UntisGridCell(merged.slot, merged.span)
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