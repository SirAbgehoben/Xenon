package org.abgehoben.xenon.ui.screens.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Info
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
import org.abgehoben.xenon.data.ClassHour
import org.abgehoben.xenon.data.MergedSlot
import org.abgehoben.xenon.data.TimetableGrid
import org.abgehoben.xenon.data.TimetableSlot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun DailyListView(
    grid: TimetableGrid,
    pagerState: PagerState,
    monday: LocalDate,
    onTabSelected: (Int) -> Unit,
    onSlotClick: (Int, MergedSlot, TimetableSlot) -> Unit
) {
    val classHours = grid.classHours

    // Observable locale from Compose configuration
    val currentLocale = LocalConfiguration.current.locales[0]
    val dFormatter = remember(currentLocale) {
        DateTimeFormatter.ofPattern("dd.MM.", currentLocale)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            divider = {}
        ) {
            for (index in 0..4) {
                val date = monday.plusDays(index.toLong())
                val name = date.dayOfWeek.getDisplayName(TextStyle.SHORT, currentLocale)
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Black else FontWeight.Medium
                            )
                            Text(
                                text = date.format(dFormatter),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val dayIdx = page + 1
            val daySlots = grid.grid[dayIdx] ?: emptyMap()
            val activeSlots = daySlots.values.filterNotNull()

            val isFullDayEvent = activeSlots.isNotEmpty() && activeSlots.size >= 5 && activeSlots.all {
                it.isHoliday || it.course == activeSlots.first().course
            }

            val daySubs = grid.substitutions.filter { sub ->
                val subDate = try {
                    if (sub.date.contains("-")) LocalDate.parse(sub.date)
                    else LocalDate.parse(sub.date, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                } catch (_: Exception) { null }
                subDate?.dayOfWeek?.value == dayIdx
            }

            val mergedSlots = getMergedSlotsForDay(daySlots, 9).filter { it.slot != null }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isFullDayEvent) {
                    val first = activeSlots.first()
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (first.isHoliday) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (first.isHoliday) Icons.Default.CalendarToday else Icons.Default.EventBusy,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = if (first.isHoliday) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = first.course,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    color = if (first.isHoliday) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.full_day_no_school),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (first.isHoliday) MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                } else if (mergedSlots.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = stringResource(R.string.no_lessons_today),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(mergedSlots) { merged ->
                        CompactLessonCard(
                            mergedSlot = merged,
                            classHours = classHours,
                            onClick = { onSlotClick(dayIdx, merged, merged.slot!!) }
                        )
                    }
                }

                if (daySubs.isNotEmpty() && !isFullDayEvent) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.daily_announcements),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    items(daySubs) { sub ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 1.dp
                        ) {
                            Text(
                                sub.text,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (sub.cancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactLessonCard(
    mergedSlot: MergedSlot,
    classHours: List<ClassHour>,
    onClick: () -> Unit
) {
    val slot = mergedSlot.slot
    val isSubstitution = slot != null && (slot.substitution != null || slot.newRoom != null || slot.subRoom != null)

    val accentColor = when {
        slot == null -> Color.Transparent
        slot.isHoliday -> MaterialTheme.colorScheme.tertiary
        isSubstitution -> Color(0xFF4CAF50)
        slot.cancelled -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }

    val colorPair = when {
        slot == null -> MaterialTheme.colorScheme.surfaceContainerLowest to MaterialTheme.colorScheme.onSurfaceVariant
        slot.isHoliday -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f) to MaterialTheme.colorScheme.onTertiaryContainer
        isSubstitution -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) to MaterialTheme.colorScheme.onSecondaryContainer
        slot.cancelled -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
    }

    val (startTime, _) = getTimeRangeForHour(mergedSlot.startHour, classHours)
    val (_, endTime) = getTimeRangeForHour(mergedSlot.endHour, classHours)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (mergedSlot.span >= 2) 66.dp else 58.dp)
            .clickable(enabled = slot != null, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = colorPair.first,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .width(4.dp)
                    .fillMaxHeight(0.7f)
                    .clip(CircleShape)
                    .background(accentColor)
            )

            Column(
                modifier = Modifier.width(48.dp).padding(start = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (mergedSlot.span > 1) "${mergedSlot.startHour}-${mergedSlot.endHour}" else "${mergedSlot.startHour}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                if (startTime.isNotEmpty()) {
                    Text(
                        text = startTime,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                if (slot != null) {
                    if (slot.cancelled) {
                        Text(
                            text = stringResource(R.string.cancelled_prefix, slot.course),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val mainText = slot.substitution ?: if (!slot.cancelled) slot.course else null
                    if (mainText != null) {
                        Text(
                            text = mainText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSubstitution && !slot.cancelled) Color(0xFF2E7D32) else colorPair.second
                        )
                    }

                    if (slot.substitution == null || !slot.cancelled) {
                        Text(
                            text = slot.teacher,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = colorPair.second.copy(alpha = 0.75f)
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.free_period),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorPair.second.copy(alpha = 0.5f)
                    )
                }
            }

            if (slot != null) {
                val r = slot.subRoom ?: slot.newRoom ?: slot.room
                if (r.isNotEmpty()) {
                    Text(
                        text = r,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, fontSize = 11.sp),
                        modifier = Modifier.padding(end = 14.dp),
                        color = if (slot.newRoom != null || slot.subRoom != null) accentColor else colorPair.second
                    )
                }
            }
        }
    }
}