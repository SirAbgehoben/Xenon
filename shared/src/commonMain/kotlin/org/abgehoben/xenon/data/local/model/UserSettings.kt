package org.abgehoben.xenon.data.local.model

import org.abgehoben.xenon.data.model.timetable.TimetableViewMode

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val defaultViewMode: TimetableViewMode = TimetableViewMode.WEEKLY,
    val mergeLessons: Boolean = true,
    val weekendAdvance: Boolean = true,
    val scaleBreaks: Boolean = true,
    val preloadWeeks: Boolean = true,
    val weeklyColWidth: Float? = null
)