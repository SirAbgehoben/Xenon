package org.abgehoben.xenon.ui.screens.timetable

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abgehoben.xenon.data.MergedSlot
import org.abgehoben.xenon.data.TimetableGrid
import org.abgehoben.xenon.data.TimetableSlot
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun UntisWeeklyGrid(
    grid: TimetableGrid,
    onSlotClick: (Int, MergedSlot, TimetableSlot) -> Unit
) {
    val hScrollState = rememberScrollState()
    val vScrollState = rememberScrollState()

    val dayNames = listOf("Mo", "Di", "Mi", "Do", "Fr")
    val dFormatter = DateTimeFormatter.ofPattern("dd.MM.")

    val colWidth = 104.dp
    val timeColWidth = 50.dp
    val totalHours = 9
    val classHours = grid.classHours

    Column(modifier = Modifier.fillMaxSize()) {
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
                Box(
                    modifier = Modifier.width(timeColWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Std.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(BlockGap))

                Row(
                    modifier = Modifier.horizontalScroll(hScrollState),
                    horizontalArrangement = Arrangement.spacedBy(BlockGap)
                ) {
                    for (i in 0..4) {
                        val date = grid.mondayDate.plusDays(i.toLong())
                        val isToday = date == LocalDate.now()

                        Box(
                            modifier = Modifier.width(colWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = dayNames[i],
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = date.format(dFormatter),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = if (isToday) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
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

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .padding(top = BlockGap, bottom = BlockGap)
        ) {
            var totalGaps = 0.dp
            for (h in 1 until totalHours) {
                val breakMin = getBreakMinutesAfter(h, classHours)
                totalGaps += getBreakGapDp(breakMin)
            }

            val baseHourHeight = maxOf(58.dp, (maxHeight - totalGaps) / totalHours)

            Box(modifier = Modifier.fillMaxSize().verticalScroll(vScrollState)) {
                Row(modifier = Modifier.fillMaxWidth()) {
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
                                Spacer(modifier = Modifier.height(getBreakGapDp(breakMin)))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(BlockGap))

                    Row(
                        modifier = Modifier.horizontalScroll(hScrollState),
                        horizontalArrangement = Arrangement.spacedBy(BlockGap)
                    ) {
                        for (d in 1..5) {
                            val daySlots = grid.grid[d] ?: emptyMap()
                            val mergedSlots = getMergedSlotsForDay(daySlots, totalHours)

                            Column(modifier = Modifier.width(colWidth)) {
                                for (merged in mergedSlots) {
                                    var blockHeight = baseHourHeight * merged.span
                                    for (i in merged.startHour until (merged.startHour + merged.span - 1)) {
                                        val breakMin = getBreakMinutesAfter(i, classHours)
                                        blockHeight += getBreakGapDp(breakMin)
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
                                        Spacer(modifier = Modifier.height(getBreakGapDp(breakMin)))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(BlockGap))
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
                                    text = "[X] ${slot.course}",
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
                                    color = if (isSubstitution && !slot.cancelled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
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
                                maxLines = 1
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
                                    text = "$span Std.",
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