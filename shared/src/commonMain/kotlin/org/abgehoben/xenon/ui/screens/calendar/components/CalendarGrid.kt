package org.abgehoben.xenon.ui.screens.calendar.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.abgehoben.xenon.data.model.calendar.ProcessedEvent
import org.abgehoben.xenon.platform.PlatformDateFormatter
import org.abgehoben.xenon.ui.theme.Dimens
import org.abgehoben.xenon.util.*
import org.abgehoben.xenon.util.YearMonth
import kotlinx.datetime.LocalDate

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    eventsByDay: Map<LocalDate, List<ProcessedEvent>>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayHeaders = remember {
        PlatformDateFormatter.getShortDayOfWeekNames()
    }

    val today = remember { LocalDate.now() }
    val daysInMonth = currentMonth.lengthOfMonth()
    val startPadding = currentMonth.atDay(1).dayOfWeek.value - 1
    val totalCells = daysInMonth + startPadding
    val rowCount = (totalCells + 6) / 7

    Column(
        modifier = modifier.padding(
            horizontal = Dimens.SpacingNormal,
            vertical = Dimens.SpacingSmall
        )
    ) {
        // Weekday abbreviations (Mon - Sun)
        Row(modifier = Modifier.fillMaxWidth()) {
            dayHeaders.forEach { dayName ->
                Text(
                    text = dayName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

        for (row in 0 until rowCount) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - startPadding + 1

                    if (dayNum in 1..daysInMonth) {
                        val date = currentMonth.atDay(dayNum)
                        val dayEvents = eventsByDay[date].orEmpty()

                        CalendarDayCell(
                            date = date,
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            hasEvents = dayEvents.isNotEmpty(),
                            hasHoliday = dayEvents.any { it.isHoliday },
                            onDateSelected = onDateSelected,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}