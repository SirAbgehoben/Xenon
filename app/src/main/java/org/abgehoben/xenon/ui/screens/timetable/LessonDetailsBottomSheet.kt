package org.abgehoben.xenon.ui.screens.timetable

import android.util.Log
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.abgehoben.xenon.R
import org.abgehoben.xenon.data.ClassHour
import org.abgehoben.xenon.data.MergedSlot
import org.abgehoben.xenon.data.TimetableSlot
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

    val currentLocale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(currentLocale) {
        DateTimeFormatter.ofPattern("E | dd.MM.yy", currentLocale)
    }

    val accentColor = when {
        slot.isHoliday -> MaterialTheme.colorScheme.tertiary
        slot.cancelled && slot.substitution == null -> MaterialTheme.colorScheme.error
        slot.substitution != null || slot.newRoom != null || slot.subRoom != null -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.primary
    }

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
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = slotDate.format(dateFormatter),
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
            IconButton(onClick = { //TODO
                Log.e(TAG, "TODO: implement 3 dot button handling")
            }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_options))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(accentColor)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            val titleText = when {
                slot.cancelled && slot.substitution != null -> stringResource(R.string.cancelled_substitution_title, slot.course, slot.substitution)
                slot.cancelled -> stringResource(R.string.cancelled_prefix, slot.course)
                slot.substitution != null -> slot.substitution
                else -> slot.course
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (slot.substitution != null && !slot.cancelled) {
                Text(
                    text = stringResource(R.string.regular_course_label, slot.course),
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

            if (slot.cancelled || slot.substitution != null || slot.newRoom != null) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = accentColor
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        val isReplacement = slot.substitution != null || slot.newRoom != null || slot.subRoom != null
                        Text(
                            text = stringResource(if (isReplacement) R.string.substitution_title else R.string.lesson_cancelled),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        if (slot.substitution != null && slot.cancelled) {
                            Text(
                                text = stringResource(R.string.replacement_prefix, slot.substitution),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (slot.subRoom != null || slot.newRoom != null) {
                            Text(
                                text = stringResource(R.string.room_change_format, slot.room, slot.subRoom ?: slot.newRoom ?: ""),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}