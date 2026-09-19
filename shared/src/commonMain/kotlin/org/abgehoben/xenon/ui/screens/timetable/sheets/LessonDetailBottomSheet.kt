package org.abgehoben.xenon.ui.screens.timetable.sheets

import org.abgehoben.xenon.platform.AppLogger
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xenon.app.generated.resources.Res
import xenon.app.generated.resources.*
import org.abgehoben.xenon.data.model.timetable.LessonStatus
import org.abgehoben.xenon.data.model.timetable.MergedSlot
import org.abgehoben.xenon.data.model.timetable.TimetableSlot
import org.abgehoben.xenon.data.remote.dto.timetable.ClassHour
import org.abgehoben.xenon.ui.screens.timetable.util.TimetableLayoutUtils.getTimeRangeForHour
import org.abgehoben.xenon.ui.screens.timetable.util.colors
import kotlinx.datetime.LocalDate
import org.abgehoben.xenon.platform.PlatformDateFormatter
import org.abgehoben.xenon.util.plusDays

private const val TAG = "LessonDetailsBottomSheet"

@Composable
fun LessonDetailsBottomSheet(
    dayIndex: Int,
    mergedSlot: MergedSlot,
    slot: TimetableSlot,
    classHours: List<ClassHour>,
    mondayDate: LocalDate,
    onDismiss: () -> Unit
) {
    val (startTime, _) = getTimeRangeForHour(mergedSlot.startHour, classHours)
    val (_, endTime) = getTimeRangeForHour(mergedSlot.endHour, classHours)
    val combinedTime = if (startTime.isNotEmpty() && endTime.isNotEmpty()) "$startTime - $endTime" else ""
    val slotDate = mondayDate.plusDays(dayIndex.toLong() - 1)

    val formattedDate = remember(slotDate) {
        PlatformDateFormatter.formatEventDate(slotDate)
    }

    val colors = slot.status.colors()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.cd_close))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (combinedTime.isNotEmpty()) {
                    Text(
                        text = combinedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = {
                AppLogger.e(TAG, "TODO: implement 3 dot button handling")
            }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(Res.string.cd_options)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(colors.accent)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            val titleText = when (slot.status) {
                LessonStatus.CANCELLED -> if (slot.substitution != null) {
                    stringResource(Res.string.cancelled_substitution_title, slot.course, slot.substitution)
                } else {
                    stringResource(Res.string.cancelled_prefix, slot.course)
                }
                LessonStatus.SUBSTITUTION -> slot.substitution ?: slot.course
                else -> slot.course
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (slot.status == LessonStatus.SUBSTITUTION && slot.substitution != slot.course) {
                Text(
                    text = stringResource(Res.string.regular_course_label, slot.course),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            val displayRoom = slot.subRoom ?: slot.newRoom ?: slot.room
            if (displayRoom.isNotEmpty()) {
                Text(
                    text = displayRoom,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            if (slot.teacher.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = slot.teacher,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            val effectiveNewRoom = slot.subRoom ?: slot.newRoom
            val hasRealRoomChange = effectiveNewRoom != null && slot.room.isNotEmpty() && effectiveNewRoom != slot.room

            if (slot.status == LessonStatus.CANCELLED || slot.status == LessonStatus.SUBSTITUTION) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = colors.accent
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(if (slot.status == LessonStatus.SUBSTITUTION) Res.string.substitution_title else Res.string.lesson_cancelled),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                        if (slot.substitution != null) {
                            Text(
                                text = stringResource(Res.string.replacement_prefix, slot.substitution),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (hasRealRoomChange) {
                            Text(
                                text = stringResource(Res.string.room_change_format, slot.room, effectiveNewRoom),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}