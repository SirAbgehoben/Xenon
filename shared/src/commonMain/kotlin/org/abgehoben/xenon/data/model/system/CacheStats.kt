package org.abgehoben.xenon.data.model.system

data class CacheStats(
    val cachedWeeksCount: Int,
    val cachedCalendarDaysCount: Int,
    val classHoursCount: Int,
    val coursesCount: Int,
    val teachersCount: Int,
    val roomsCount: Int
)