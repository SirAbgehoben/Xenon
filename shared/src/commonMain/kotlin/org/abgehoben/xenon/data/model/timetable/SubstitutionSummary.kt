package org.abgehoben.xenon.data.model.timetable

data class SubstitutionSummary(
    val date: String,
    val dayName: String,
    val hours: String,
    val cancelled: Boolean,
    val text: String
)