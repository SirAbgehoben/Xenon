package org.abgehoben.xenon.ui.screens.timetable.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abgehoben.xenon.data.remote.dto.timetable.ClassHour
import org.abgehoben.xenon.ui.screens.timetable.util.TimetableLayoutUtils
import org.abgehoben.xenon.ui.theme.Dimens

@Composable
fun TimetablePeriodColumn(
    totalHours: Int,
    classHours: List<ClassHour>,
    baseHourHeight: Dp,
    scaleBreaks: Boolean,
    modifier: Modifier = Modifier,
    startHour: Int = 1,
    periodDurationMinutes: Long = 45L
) {
    Column(modifier = modifier) {
        for (h in startHour..totalHours) {
            val (startTime, endTime) = TimetableLayoutUtils.getTimeRangeForHour(h, classHours)

            PeriodIndicatorCell(
                periodNumber = h,
                startTime = startTime,
                endTime = endTime,
                height = baseHourHeight
            )

            if (h < totalHours) {
                val breakMin = TimetableLayoutUtils.getBreakMinutesAfter(h, classHours)
                val breakGap = TimetableLayoutUtils.getBreakGapDp(
                    breakMinutes = breakMin,
                    scaleBreaks = scaleBreaks,
                    baseHourHeight = baseHourHeight,
                    periodDurationMinutes = periodDurationMinutes
                )
                Spacer(modifier = Modifier.height(breakGap))
            }
        }
    }
}

@Composable
private fun PeriodIndicatorCell(
    periodNumber: Int,
    startTime: String,
    endTime: String,
    height: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(height)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$periodNumber",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (startTime.isNotEmpty() && endTime.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.SpacingHairline))
                Text(
                    text = "$startTime\n$endTime",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        lineHeight = 9.5.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}