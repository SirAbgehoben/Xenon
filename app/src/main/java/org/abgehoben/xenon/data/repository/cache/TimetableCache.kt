package org.abgehoben.xenon.data.repository.cache

import org.abgehoben.xenon.data.model.timetable.TimetableGrid
import org.abgehoben.xenon.data.remote.dto.timetable.ClassHour
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class TimetableCache {
    private val gridCache = ConcurrentHashMap<LocalDate, TimetableGrid>()
    private val classHoursCache = AtomicReference<List<ClassHour>?>(null)

    fun getGrid(monday: LocalDate): TimetableGrid? = gridCache[monday]

    fun putGrid(monday: LocalDate, grid: TimetableGrid) {
        gridCache[monday] = grid
    }

    fun containsGrid(monday: LocalDate): Boolean = gridCache.containsKey(monday)

    fun getClassHours(): List<ClassHour>? = classHoursCache.get()

    fun putClassHours(classHours: List<ClassHour>) {
        classHoursCache.set(classHours)
    }

    fun getCachedWeeksCount(): Int = gridCache.size

    fun getClassHoursCount(): Int = classHoursCache.get()?.size ?: 0

    fun clear() {
        gridCache.clear()
        classHoursCache.set(null)
    }
}