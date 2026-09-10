// main/java/org/abgehoben/xenon/ui/screens/timetable/UntisWeeklyGrid.kt
package org.abgehoben.xenon.ui.screens.timetable

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abgehoben.xenon.R
import org.abgehoben.xenon.data.MergedSlot
import org.abgehoben.xenon.data.TimetableGrid
import org.abgehoben.xenon.data.TimetableSlot
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

    val colWidth = 104.dp
    val timeColWidth = 50.dp
    val totalHours = 9
    val classHours = grid.classHours

    val nowTime = rememberLiveTime()
    val today = LocalDate.now()
    val isCurrentWeek = !today.isBefore(grid.mondayDate) && !today.isAfter(grid.mondayDate.plusDays(4))
    val currentDayIndex = today.dayOfWeek.value

    val dynamicPrimary = MaterialTheme.colorScheme.primary
    val dynamicPrimaryContainer = MaterialTheme.colorScheme.primaryContainer
    val dynamicOnPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Column(modifier = Modifier.fillMaxSize()) {
        // Pinned Header Row
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = BlockGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pinned "Per." / "Std." Column Header
                Box(
                    modifier = Modifier.width(timeColWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.period_abbr),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(BlockGap))

                // Scrollable Day Header Pills
                Row(
                    modifier = Modifier.horizontalScroll(hScrollState),
                    horizontalArrangement = Arrangement.spacedBy(BlockGap)
                ) {
                    for (i in 0..4) {
                        val date = grid.mondayDate.plusDays(i.toLong())
                        val isToday = date == today
                        val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, currentLocale)

                        Box(
                            modifier = Modifier.width(colWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isToday) dynamicPrimaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isToday) BorderStroke(1.5.dp, dynamicPrimary) else null,
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
                    Spacer(modifier = Modifier.width(BlockGap))
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        // Body: Pinned Time Column + Scrollable Day Cards
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .padding(top = BlockGap, bottom = BlockGap)
        ) {
            var totalGaps = 0.dp
            for (h in 1 until totalHours) {
                val breakMin = getBreakMinutesAfter(h, classHours)
                totalGaps += getBreakGapDp(breakMin, scaleBreaks)
            }

            val baseHourHeight = maxOf(58.dp, (maxHeight - totalGaps) / totalHours)

            val liveYOffset = remember(nowTime, baseHourHeight, scaleBreaks) {
                calculateCurrentTimeYOffset(nowTime, totalHours, classHours, baseHourHeight, scaleBreaks)
            }

            Box(modifier = Modifier.fillMaxSize().verticalScroll(vScrollState)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Pinned Timing Column on the left
                    Column(modifier = Modifier.width(timeColWidth)) {
                        for (h in 1..totalHours) {
                            val (startTime, endTime) = getTimeRangeForHour(h, classHours)
                            Box(
                                modifier = Modifier
                                    .height(baseHourHeight)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "$h",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$startTime\n$endTime",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 9.5.sp),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            if (h < totalHours) {
                                val breakMin = getBreakMinutesAfter(h, classHours)
                                Spacer(modifier = Modifier.height(getBreakGapDp(breakMin, scaleBreaks)))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(BlockGap))

                    // Scrollable Day Columns
                    Box(modifier = Modifier.horizontalScroll(hScrollState)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(BlockGap)) {
                            for (d in 1..5) {
                                val daySlots = grid.grid[d] ?: emptyMap()
                                val mergedSlots = getMergedSlotsForDay(daySlots, totalHours, mergeLessons)

                                Column(modifier = Modifier.width(colWidth)) {
                                    for (merged in mergedSlots) {
                                        var blockHeight = baseHourHeight * merged.span
                                        for (i in merged.startHour until (merged.startHour + merged.span - 1)) {
                                            val breakMin = getBreakMinutesAfter(i, classHours)
                                            blockHeight += getBreakGapDp(breakMin, scaleBreaks)
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
                                            val breakMin = getBreakMinutesAfter(merged.endHour, classHours)
                                            Spacer(modifier = Modifier.height(getBreakGapDp(breakMin, scaleBreaks)))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(BlockGap))
                        }

                        if (isCurrentWeek && liveYOffset != null) {
                            LiveTimeIndicatorOverlay(
                                yOffset = liveYOffset,
                                currentDayIndex = currentDayIndex,
                                colWidth = colWidth,
                                blockGap = BlockGap,
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

@Composable
fun UntisGridCell(slot: TimetableSlot, span: Int = 1) {
    val isSubstitution = slot.substitution != null || slot.newRoom != null || slot.subRoom != null

    val accentColor = when {
        slot.isHoliday -> MaterialTheme.colorScheme.tertiary
        slot.cancelled && !isSubstitution -> MaterialTheme.colorScheme.error
        isSubstitution -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.primary
    }

    val bgColor = when {
        slot.isHoliday -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
        slot.cancelled && !isSubstitution -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        isSubstitution -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        tonalElevation = 1.dp
    ) {
        if (slot.isHoliday) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (span >= 2) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = slot.course,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = if (span >= 2) 11.5.sp else 9.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center,
                        maxLines = if (span >= 2) 4 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .width(3.5.dp)
                        .fillMaxHeight(0.75f)
                        .clip(CircleShape)
                        .background(accentColor)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp, vertical = if (span >= 2) 8.dp else 4.dp),
                    verticalArrangement = if (span >= 2) Arrangement.SpaceEvenly else Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (slot.cancelled) {
                                Text(
                                    text = stringResource(R.string.cancelled_prefix, slot.course),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val displayText = slot.substitution ?: if (!slot.cancelled) slot.course else null
                            if (displayText != null) {
                                Text(
                                    text = displayText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = if (span >= 2) 11.sp else 9.5.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    maxLines = if (span >= 2) 2 else 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (slot.teacher.isNotEmpty()) {
                            Text(
                                text = slot.teacher,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = if (span >= 2) 9.5.sp else 8.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .widthIn(max = 38.dp)
                            )
                        }
                    }

                    val rName = slot.subRoom ?: slot.newRoom ?: slot.room
                    if (rName.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = if (span >= 2) 10.5.sp else 9.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = if (slot.newRoom != null || slot.subRoom != null) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (span >= 2) {
                                Text(
                                    text = stringResource(R.string.periods_count, span),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}