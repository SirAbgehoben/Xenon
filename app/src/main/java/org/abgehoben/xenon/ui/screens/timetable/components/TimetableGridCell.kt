package org.abgehoben.xenon.ui.screens.timetable.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abgehoben.xenon.R
import org.abgehoben.xenon.data.model.timetable.TimetableSlot
import org.abgehoben.xenon.ui.theme.Dimens
import org.abgehoben.xenon.ui.theme.StatusSubstitution

@Composable
fun TimetableGridCell(slot: TimetableSlot, span: Int = 1) {
    val isSubstitution = slot.substitution != null || slot.newRoom != null || slot.subRoom != null

    val accentColor = when {
        slot.isHoliday -> MaterialTheme.colorScheme.tertiary
        slot.cancelled && !isSubstitution -> MaterialTheme.colorScheme.error
        isSubstitution -> StatusSubstitution
        else -> MaterialTheme.colorScheme.primary
    }

    val bgColor = when {
        slot.isHoliday -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
        slot.cancelled && !isSubstitution -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        isSubstitution -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(Dimens.RadiusMedium),
        color = bgColor,
        tonalElevation = 1.dp
    ) {
        if (slot.isHoliday) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.SpacingStandard),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (span >= 2) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(Dimens.IconSizeMedium)
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                    }
                    Text(
                        text = slot.course,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = if (span >= 2) 11.5.sp else 9.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center,
                        maxLines = if (span >= 2) 4 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(start = Dimens.SpacingExtraSmall)
                        .width(3.5.dp)
                        .fillMaxHeight(0.75f)
                        .clip(CircleShape)
                        .background(accentColor)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            horizontal = Dimens.SpacingSmall,
                            vertical = if (span >= 2) Dimens.SpacingStandard else Dimens.SpacingExtraSmall
                        ),
                    verticalArrangement = if (span >= 2) Arrangement.SpaceEvenly else Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (slot.cancelled) {
                                Text(
                                    text = stringResource(R.string.cancelled_prefix, slot.course),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val displayText =
                                slot.substitution ?: if (!slot.cancelled) slot.course else null
                            if (displayText != null) {
                                Text(
                                    text = displayText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = if (span >= 2) 11.sp else 9.5.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    maxLines = if (span >= 2) 2 else 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (slot.teacher.isNotEmpty()) {
                            Text(
                                text = slot.teacher,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = if (span >= 2) 9.5.sp else 8.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(start = Dimens.SpacingExtraSmall)
                                    .widthIn(max = 38.dp)
                            )
                        }
                    }

                    val rName = slot.subRoom ?: slot.newRoom ?: slot.room
                    if (rName.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = if (span >= 2) 10.5.sp else 9.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = if (slot.newRoom != null || slot.subRoom != null) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (span >= 2) {
                                Text(
                                    text = stringResource(R.string.periods_count, span),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}