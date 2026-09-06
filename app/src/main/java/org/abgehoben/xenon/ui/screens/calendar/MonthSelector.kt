package org.abgehoben.xenon.ui.screens.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.abgehoben.xenon.R
import java.time.YearMonth
import java.time.format.TextStyle

@Composable
fun MonthSelector(
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit
) {
    val currentLocale = LocalConfiguration.current.locales[0]
    val monthTitle = remember(currentMonth, currentLocale) {
        "${currentMonth.month.getDisplayName(TextStyle.FULL, currentLocale)} ${currentMonth.year}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(
            onClick = { onMonthChange(currentMonth.minusMonths(1)) },
            modifier = Modifier.size(36.dp),
            shape = CircleShape
        ) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = stringResource(R.string.cd_prev_month),
                modifier = Modifier.size(18.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp
        ) {
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        FilledTonalIconButton(
            onClick = { onMonthChange(currentMonth.plusMonths(1)) },
            modifier = Modifier.size(36.dp),
            shape = CircleShape
        ) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.cd_next_month),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}