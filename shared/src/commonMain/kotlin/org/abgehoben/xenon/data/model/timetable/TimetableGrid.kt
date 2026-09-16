package org.abgehoben.xenon.data.model.timetable

import org.abgehoben.xenon.data.remote.dto.timetable.ClassHour
import java.time.LocalDate

data class TimetableGrid(
    val calWeek: Int,
    val weekType: String,
    val mondayDate: LocalDate,
    val grid: Map<Int, Map<Int, TimetableSlot?>>,
    val substitutions: List<SubstitutionSummary>,
    val classHours: List<ClassHour> = emptyList()
)