package org.abgehoben.xenon.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.text.HtmlCompat
import org.abgehoben.xenon.R
import org.abgehoben.xenon.data.model.calendar.ProcessedEvent
import org.abgehoben.xenon.ui.theme.Dimens
import java.time.format.DateTimeFormatter

@Composable
fun EventDetailsBottomSheet(
    event: ProcessedEvent,
    onDismiss: () -> Unit
) {
    val accentColor = if (event.isHoliday) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    val currentLocale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(currentLocale) {
        DateTimeFormatter.ofPattern("E | dd.MM.yy", currentLocale)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.SpacingJumbo)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingStandard, vertical = Dimens.SpacingExtraSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = event.startDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (event.allDay) {
                        stringResource(R.string.all_day)
                    } else if (event.endTime.isNotEmpty()) {
                        "${event.startTime} - ${event.endTime}"
                    } else {
                        event.startTime
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Keep spacer to balance the close button
            Spacer(modifier = Modifier.size(Dimens.IconSizeLarge))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.SpacingExtraSmall)
                .background(accentColor)
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingLarge))

        Column(modifier = Modifier.padding(horizontal = Dimens.SpacingExtraLarge)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (event.category.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.SpacingHairline))
                Text(
                    text = event.category,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (event.location.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.SpacingNormal))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconSizeMedium),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingStandard))
                    Text(text = event.location, style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (event.organizer.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.SpacingStandard))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconSizeMedium),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingStandard))
                    Text(text = event.organizer, style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (event.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.SpacingLarge))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(Dimens.SpacingLarge))

                Row {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconSizeMedium),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingStandard))
                    val cleanText = remember(event.description) {
                        HtmlCompat.fromHtml(event.description, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
                    }
                    Text(text = cleanText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}