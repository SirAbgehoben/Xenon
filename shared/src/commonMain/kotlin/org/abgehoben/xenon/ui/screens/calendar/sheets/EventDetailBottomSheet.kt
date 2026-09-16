package org.abgehoben.xenon.ui.screens.calendar.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import org.abgehoben.xenon.data.model.calendar.ProcessedEvent
import org.abgehoben.xenon.ui.theme.Dimens
import java.time.format.DateTimeFormatter

@Composable
fun EventDetailsBottomSheet(
    event: ProcessedEvent,
    onDismiss: () -> Unit
) {
    val accentColor = if (event.isHoliday) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    val currentLocale = androidx.compose.ui.text.intl.Locale.current.platformLocale
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
                Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.cd_close))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = event.startDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (event.allDay) {
                        stringResource(Res.string.all_day)
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
                        stripHtml(event.description)
                    }
                    Text(text = cleanText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

//TODO: Temporary for now; apparently .HtmlCompat is android only ¯\_(ツ)_/¯
private fun stripHtml(html: String): String {
    return html
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<p.*?>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .trim()
}