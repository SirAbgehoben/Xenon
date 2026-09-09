package org.abgehoben.xenon.data.repository.cache

import org.abgehoben.xenon.data.model.calendar.ProcessedEvent
import org.abgehoben.xenon.data.model.system.CacheStats
import org.abgehoben.xenon.data.model.timetable.SchoolMetadata
import org.abgehoben.xenon.data.model.timetable.TimetableGrid
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class TimetableCache {
    private val timetableCache = ConcurrentHashMap<LocalDate, TimetableGrid>()
    private val cachedMetadata = AtomicReference<SchoolMetadata?>(null)
    private val cachedCalendarEvents = AtomicReference<Map<LocalDate, List<ProcessedEvent>>?>(null)

    fun getGrid(monday: LocalDate): TimetableGrid? = timetableCache[monday]

    fun putGrid(monday: LocalDate, grid: TimetableGrid) {
        timetableCache[monday] = grid
    }

    fun containsGrid(monday: LocalDate): Boolean = timetableCache.containsKey(monday)

    fun getMetadata(): SchoolMetadata? = cachedMetadata.get()

    fun putMetadata(metadata: SchoolMetadata) {
        cachedMetadata.set(metadata)
    }

    fun getCalendarEvents(): Map<LocalDate, List<ProcessedEvent>>? = cachedCalendarEvents.get()

    fun putCalendarEvents(events: Map<LocalDate, List<ProcessedEvent>>) {
        cachedCalendarEvents.set(events)
    }

    fun getStats(): CacheStats {
        val meta = cachedMetadata.get()
        return CacheStats(
            cachedWeeksCount = timetableCache.size,
            cachedCalendarDaysCount = cachedCalendarEvents.get()?.size ?: 0,
            classHoursCount = meta?.classHours?.size ?: 0,
            coursesCount = meta?.courses?.size ?: 0,
            teachersCount = meta?.teachers?.size ?: 0,
            roomsCount = meta?.rooms?.size ?: 0
        )
    }

    fun clear() {
        timetableCache.clear()
        cachedMetadata.set(null)
        cachedCalendarEvents.set(null)
    }
}