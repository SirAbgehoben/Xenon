package org.abgehoben.xenon.ui.screens.timetable.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abgehoben.xenon.R
import org.abgehoben.xenon.data.model.timetable.MergedSlot
import org.abgehoben.xenon.data.remote.dto.timetable.ClassHour
import org.abgehoben.xenon.ui.screens.timetable.util.TimetableLayoutUtils
import org.abgehoben.xenon.ui.theme.Dimens
import org.abgehoben.xenon.ui.theme.StatusSubstitution

@Composable
fun CompactLessonCard(
    mergedSlot: MergedSlot,
    classHours: List<ClassHour>,
    isCurrentLesson: Boolean = false,
    currentProgress: Float = 0f,
    onClick: () -> Unit
) {
    val slot = mergedSlot.slot
    val isSubstitution = slot != null && (slot.substitution != null || slot.newRoom != null || slot.subRoom != null)

    val accentColor = when {
        slot == null -> Color.Transparent
        slot.isHoliday -> MaterialTheme.colorScheme.tertiary
        isSubstitution -> StatusSubstitution
        slot.cancelled -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    val colorPair = when {
        slot == null -> MaterialTheme.colorScheme.surfaceContainerLowest to MaterialTheme.colorScheme.onSurfaceVariant
        slot.isHoliday -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f) to MaterialTheme.colorScheme.onTertiaryContainer
        isSubstitution -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) to MaterialTheme.colorScheme.onSecondaryContainer
        slot.cancelled -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
    }

    val (startTime, _) = TimetableLayoutUtils.getTimeRangeForHour(mergedSlot.startHour, classHours)
    val cardHeight = if (mergedSlot.span >= 2) Dimens.TimetableMergedHourHeight else Dimens.TimetableMinHourHeight

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = slot != null, onClick = onClick),
            shape = RoundedCornerShape(Dimens.RadiusStandard),
            color = colorPair.first,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = Dimens.SpacingExtraSmall)
                        .width(4.dp)
                        .fillMaxHeight(0.7f)
                        .clip(CircleShape)
                        .background(accentColor)
                )

                Column(
                    modifier = Modifier
                        .width(48.dp)
                        .padding(start = Dimens.SpacingSmall),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (mergedSlot.span > 1) "${mergedSlot.startHour}-${mergedSlot.endHour}" else "${mergedSlot.startHour}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    if (startTime.isNotEmpty()) {
                        Text(
                            text = startTime,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = Dimens.SpacingSmall)
                ) {
                    if (slot != null) {
                        if (slot.cancelled) {
                            Text(
                                text = stringResource(R.string.cancelled_prefix, slot.course),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        val mainText =
                            slot.substitution ?: if (!slot.cancelled) slot.course else null
                        if (mainText != null) {
                            Text(
                                text = mainText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (slot.substitution == null || !slot.cancelled) {
                            Text(
                                text = slot.teacher,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = colorPair.second.copy(alpha = 0.75f)
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.free_period),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorPair.second.copy(alpha = 0.5f)
                        )
                    }
                }

                if (slot != null) {
                    val r = slot.subRoom ?: slot.newRoom ?: slot.room
                    if (r.isNotEmpty()) {
                        Text(
                            text = r,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(end = Dimens.SpacingNormal),
                            color = if (slot.newRoom != null || slot.subRoom != null) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (isCurrentLesson && currentProgress in 0f..1f) {
            val lineY = cardHeight * currentProgress
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = lineY - (Dimens.TimeIndicatorHeight / 2))
                    .height(Dimens.TimeIndicatorHeight)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}