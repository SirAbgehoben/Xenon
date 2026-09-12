package org.abgehoben.xenon.ui.screens.timetable.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.abgehoben.xenon.R
import org.abgehoben.xenon.data.model.timetable.MergedSlot
import org.abgehoben.xenon.data.model.timetable.TimetableGrid
import org.abgehoben.xenon.data.model.timetable.TimetableSlot
import org.abgehoben.xenon.ui.screens.timetable.components.CompactLessonCard
import org.abgehoben.xenon.ui.screens.timetable.components.rememberLiveTime
import org.abgehoben.xenon.ui.screens.timetable.util.TimetableLayoutUtils
import org.abgehoben.xenon.ui.theme.Dimens
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun DailyListView(
    grid: TimetableGrid,
    pagerState: PagerState,
    monday: LocalDate,
    mergeLessons: Boolean = true,
    scaleBreaks: Boolean = true,
    onTabSelected: (Int) -> Unit,
    onSlotClick: (Int, MergedSlot, TimetableSlot) -> Unit
) {
    val classHours = grid.classHours
    val nowTime = rememberLiveTime()
    val today = LocalDate.now()

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
            val pageDate = monday.plusDays(page.toLong())
            val isToday = pageDate == today

            val daySlots = grid.grid[dayIdx] ?: emptyMap()
            val activeSlots = daySlots.values.filterNotNull()

            val isFullDayEvent =
                activeSlots.isNotEmpty() && activeSlots.size >= 5 && activeSlots.all {
                    it.isHoliday || it.course == activeSlots.first().course
                }

            val daySubs = grid.substitutions.filter { sub ->
                val subDate = try {
                    if (sub.date.contains("-")) LocalDate.parse(sub.date)
                    else LocalDate.parse(sub.date, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                } catch (_: Exception) {
                    null
                }
                subDate?.dayOfWeek?.value == dayIdx
            }.distinctBy { it.text }

            val maxDayHour = maxOf(
                classHours.maxOfOrNull { it.number } ?: 0,
                daySlots.filterValues { it != null }.keys.maxOrNull() ?: 0
            ).takeIf { it > 0 } ?: 9

            val mergedSlots =
                TimetableLayoutUtils.getMergedSlotsForDay(daySlots, maxDayHour, mergeLessons)
                    .filter { it.slot != null }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Dimens.SpacingLarge)
            ) {
                if (isFullDayEvent) {
                    val first = activeSlots.first()
                    item {
                        Surface(
                            shape = RoundedCornerShape(Dimens.RadiusCard),
                            color = if (first.isHoliday) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer.copy(
                                alpha = 0.7f
                            ),
                            tonalElevation = Dimens.ElevationLevel2,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(Dimens.SpacingCard),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (first.isHoliday) Icons.Default.CalendarToday else Icons.Default.EventBusy,
                                    contentDescription = null,
                                    modifier = Modifier.size(Dimens.IconSizeBig),
                                    tint = if (first.isHoliday) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                                Text(
                                    text = first.course,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    color = if (first.isHoliday) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                                Text(
                                    text = stringResource(R.string.full_day_no_school),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (first.isHoliday) MaterialTheme.colorScheme.onTertiaryContainer.copy(
                                        alpha = 0.8f
                                    ) else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                } else if (mergedSlots.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimens.SpacingLarge),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.RadiusLarge),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = stringResource(R.string.no_lessons_today),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.SpacingExtraLarge),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(mergedSlots) { index, merged ->
                        val (startStr, _) = TimetableLayoutUtils.getTimeRangeForHour(
                            merged.startHour,
                            classHours
                        )
                        val (_, endStr) = TimetableLayoutUtils.getTimeRangeForHour(
                            merged.endHour,
                            classHours
                        )
                        val lessonStart = runCatching { LocalTime.parse(startStr) }.getOrNull()
                        val lessonEnd = runCatching { LocalTime.parse(endStr) }.getOrNull()

                        val isCurrentLesson = isToday && lessonStart != null && lessonEnd != null &&
                                !nowTime.isBefore(lessonStart) && !nowTime.isAfter(lessonEnd)

                        val currentProgress = if (isCurrentLesson) {
                            val totalSec =
                                Duration.between(lessonStart, lessonEnd).seconds.coerceAtLeast(1)
                            val elapsedSec = Duration.between(lessonStart, nowTime).seconds
                            (elapsedSec.toFloat() / totalSec).coerceIn(0f, 1f)
                        } else 0f

                        CompactLessonCard(
                            mergedSlot = merged,
                            classHours = classHours,
                            isCurrentLesson = isCurrentLesson,
                            currentProgress = currentProgress,
                            onClick = { onSlotClick(dayIdx, merged, merged.slot!!) }
                        )

                        if (index < mergedSlots.size - 1) {
                            val nextMerged = mergedSlots[index + 1]
                            val breakMin = TimetableLayoutUtils.getBreakMinutesAfter(
                                merged.endHour,
                                classHours
                            )
                            val gapDp = TimetableLayoutUtils.getBreakGapDp(breakMin, scaleBreaks)

                            val nextStartStr = TimetableLayoutUtils.getTimeRangeForHour(
                                nextMerged.startHour,
                                classHours
                            ).first
                            val nextStart =
                                runCatching { LocalTime.parse(nextStartStr) }.getOrNull()

                            val isCurrentBreak =
                                isToday && lessonEnd != null && nextStart != null &&
                                        nowTime.isAfter(lessonEnd) && nowTime.isBefore(nextStart)

                            val breakProgress = if (isCurrentBreak) {
                                val totalSec =
                                    Duration.between(lessonEnd, nextStart).seconds.coerceAtLeast(1)
                                val elapsedSec = Duration.between(lessonEnd, nowTime).seconds
                                (elapsedSec.toFloat() / totalSec).coerceIn(0f, 1f)
                            } else 0f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(gapDp)
                            ) {
                                if (isCurrentBreak) {
                                    val lineY = gapDp * breakProgress
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .offset(y = lineY - (Dimens.TimeIndicatorHeight / 2))
                                            .height(Dimens.TimeIndicatorHeight)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                }

                if (daySubs.isNotEmpty() && !isFullDayEvent) {
                    item {
                        Spacer(modifier = Modifier.height(Dimens.SpacingLarge))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.IconSizeSmall),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
                            Text(
                                text = stringResource(R.string.daily_announcements),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                    }
                    itemsIndexed(daySubs) { subIdx, sub ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.RadiusMedium),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = Dimens.ElevationLevel1
                        ) {
                            Text(
                                text = sub.text,
                                modifier = Modifier.padding(Dimens.SpacingMedium),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (sub.cancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (subIdx < daySubs.size - 1) {
                            Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                        }
                    }
                }
            }
        }
    }
}