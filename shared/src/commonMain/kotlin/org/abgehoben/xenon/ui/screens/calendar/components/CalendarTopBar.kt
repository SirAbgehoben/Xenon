package org.abgehoben.xenon.ui.screens.calendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.util.Locale
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import xenon.app.generated.resources.Res
import xenon.app.generated.resources.*
import org.abgehoben.xenon.ui.theme.Dimens
import java.time.YearMonth
import java.time.format.TextStyle

@Composable
fun MonthSelector(
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLocale = androidx.compose.ui.text.intl.Locale.current.platformLocale
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
                contentDescription = stringResource(Res.string.cd_prev_month),
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
                contentDescription = stringResource(Res.string.cd_next_month),
                modifier = Modifier.size(Dimens.ActionIconSize)
            )
        }
    }
}