package org.abgehoben.xenon.ui.screens.timetable.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import kotlinx.datetime.LocalDate
import org.abgehoben.xenon.ui.theme.Dimens
import org.abgehoben.xenon.util.now
import org.abgehoben.xenon.util.plusDays
import org.jetbrains.compose.resources.stringResource
import xenon.app.generated.resources.Res
import xenon.app.generated.resources.period_abbr

@Composable
fun TimetableDayHeaderRow(
    mondayDate: LocalDate,
    scrollState: ScrollState,
    onDayClick: (dayOffset: Int) -> Unit,
    modifier: Modifier = Modifier,
    colWidth: Dp = Dimens.TimetableColWidth,
    daysCount: Int = 5,
    onCornerClick: (() -> Unit)? = null
) {
    val today = remember { LocalDate.now() }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = Dimens.ElevationLevel1,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.TimetableBlockGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(Dimens.TimetableTimeColWidth)
                    .then(
                        if (onCornerClick != null) Modifier.clickable(onClick = onCornerClick)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.period_abbr),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(Dimens.TimetableBlockGap))

            Row(
                modifier = Modifier.horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(Dimens.TimetableBlockGap)
            ) {
                for (dayOffset in 0 until daysCount) {
                    val date = remember(mondayDate, dayOffset) { mondayDate.plusDays(dayOffset.toLong()) }
                    val isToday = date == today

                    Box(
                        modifier = Modifier.width(colWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        TimetableDayHeaderCell(
                            date = date,
                            isToday = isToday,
                            onClick = { onDayClick(dayOffset) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(Dimens.TimetableBlockGap))
            }
        }
    }
}