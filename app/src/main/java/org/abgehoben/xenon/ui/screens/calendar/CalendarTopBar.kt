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
import org.abgehoben.xenon.R
import org.abgehoben.xenon.ui.theme.Dimens
import java.time.YearMonth
import java.time.format.TextStyle

@Composable
fun MonthSelector(
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLocale = LocalConfiguration.current.locales[0]
    val monthTitle = remember(currentMonth, currentLocale) {
        "${currentMonth.month.getDisplayName(TextStyle.FULL, currentLocale)} ${currentMonth.year}"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(
            onClick = { onMonthChange(currentMonth.minusMonths(1)) },
            modifier = Modifier.size(Dimens.IconSizeBig),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = stringResource(R.string.cd_prev_month),
                modifier = Modifier.size(Dimens.ActionIconSize)
            )
        }

        Surface(
            shape = RoundedCornerShape(Dimens.RadiusLarge),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = Dimens.ElevationLevel2
        ) {
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    horizontal = Dimens.SpacingLarge,
                    vertical = Dimens.SpacingSmall
                )
            )
        }

        FilledTonalIconButton(
            onClick = { onMonthChange(currentMonth.plusMonths(1)) },
            modifier = Modifier.size(Dimens.IconSizeBig),
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.cd_next_month),
                modifier = Modifier.size(Dimens.ActionIconSize)
            )
        }
    }
}