package org.abgehoben.xenon.ui.screens.timetable.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abgehoben.xenon.platform.PlatformDateFormatter
import org.abgehoben.xenon.ui.theme.Dimens
import kotlinx.datetime.LocalDate

@Composable
fun TimetableDayHeaderCell(
    date: LocalDate,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayName = remember(date) {
        PlatformDateFormatter.formatShortDayOfWeek(date)
    }
    val formattedDate = remember(date) {
        PlatformDateFormatter.formatDayAndMonth(date)
    }

    val dynamicPrimary = MaterialTheme.colorScheme.primary
    val dynamicPrimaryContainer = MaterialTheme.colorScheme.primaryContainer
    val dynamicOnPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.RadiusMedium),
        color = if (isToday) dynamicPrimaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (isToday) BorderStroke(Dimens.StrokeMedium, dynamicPrimary) else null,
        modifier = modifier
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
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = if (isToday) dynamicPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}