package org.abgehoben.xenon.ui.screens.timetable.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xenon.app.generated.resources.Res
import xenon.app.generated.resources.*
import org.abgehoben.xenon.data.model.timetable.LessonStatus
import org.abgehoben.xenon.data.model.timetable.TimetableSlot
import org.abgehoben.xenon.ui.screens.timetable.util.colors
import org.abgehoben.xenon.ui.theme.Dimens

@Composable
fun TimetableGridCell(
    slot: TimetableSlot,
    span: Int = 1,
    isCompact: Boolean = false
) {
    val colors = slot.status.colors()

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(Dimens.RadiusMedium),
        color = colors.container,
        tonalElevation = 1.dp
    ) {
        if (slot.status == LessonStatus.HOLIDAY) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isCompact) Dimens.SpacingExtraSmall else Dimens.SpacingStandard),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (span >= 2 && !isCompact) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = colors.onContainer,
                            modifier = Modifier.size(Dimens.IconSizeMedium)
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                    }
                    Text(
                        text = slot.course,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = if (span >= 2) (if (isCompact) 9.5.sp else 11.5.sp) else (if (isCompact) 8.sp else 9.sp),
                            fontWeight = FontWeight.Black
                        ),
                        color = colors.onContainer,
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
                        .width(if (isCompact) 2.5.dp else 3.5.dp)
                        .fillMaxHeight(0.75f)
                        .clip(CircleShape)
                        .background(colors.accent)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            horizontal = if (isCompact) Dimens.SpacingExtraSmall else Dimens.SpacingSmall,
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
                            if (slot.status == LessonStatus.CANCELLED) {
                                Text(
                                    text = stringResource(Res.string.cancelled_prefix, slot.course),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = if (isCompact) 7.5.sp else 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val displayText = slot.substitution ?: if (slot.status != LessonStatus.CANCELLED) slot.course else null
                            if (displayText != null) {
                                Text(
                                    text = displayText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = if (span >= 2) (if (isCompact) 10.sp else 11.sp) else (if (isCompact) 8.5.sp else 9.5.sp),
                                        fontWeight = FontWeight.Black
                                    ),
                                    maxLines = if (span >= 2) 2 else 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // When squished compact, hide teacher on 1-hour slots so subject name remains readable
                        if (slot.teacher.isNotEmpty() && (!isCompact || span >= 2)) {
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
                                    .widthIn(max = if (isCompact) 28.dp else 38.dp)
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
                                    fontSize = if (span >= 2) (if (isCompact) 9.5.sp else 10.5.sp) else (if (isCompact) 8.sp else 9.sp),
                                    fontWeight = FontWeight.Black
                                ),
                                color = if (slot.status == LessonStatus.SUBSTITUTION) colors.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (span >= 2 && !isCompact) {
                                Text(
                                    text = stringResource(Res.string.periods_count, span),
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