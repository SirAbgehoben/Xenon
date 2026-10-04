package org.abgehoben.xenon.data.model.timetable

import androidx.compose.runtime.Immutable

@Immutable
data class MergedSlot(
    val startHour: Int,
    val span: Int,
    val slot: TimetableSlot?,
    val endHour: Int = startHour + span - 1
) {
    val status: LessonStatus
        get() = slot?.status ?: LessonStatus.FREE_PERIOD
}