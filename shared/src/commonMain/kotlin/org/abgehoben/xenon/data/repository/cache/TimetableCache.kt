package org.abgehoben.xenon.data.repository.cache

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import org.abgehoben.xenon.data.model.timetable.TimetableGrid
import org.abgehoben.xenon.data.remote.dto.timetable.ClassHour
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalAtomicApi::class)
class TimetableCache {
    private val gridCache = AtomicReference<Map<LocalDate, TimetableGrid>>(emptyMap())
    private val classHoursCache = AtomicReference<List<ClassHour>?>(null)

    fun getGrid(monday: LocalDate): TimetableGrid? = gridCache.load()[monday]

    fun putGrid(monday: LocalDate, grid: TimetableGrid) {
        // Lock-free atomic CAS update to prevent lost writes during concurrent week preloading
        while (true) {
            val current = gridCache.load()
            val updated = current + (monday to grid)
            if (gridCache.compareAndSet(current, updated)) {
                break
            }
        }
    }

    fun containsGrid(monday: LocalDate): Boolean = gridCache.load().containsKey(monday)

    fun getClassHours(): List<ClassHour>? = classHoursCache.load()

    fun putClassHours(classHours: List<ClassHour>) {
        classHoursCache.store(classHours)
    }

    fun getCachedWeeksCount(): Int = gridCache.load().size

    fun getClassHoursCount(): Int = classHoursCache.load()?.size ?: 0

    fun clear() {
        gridCache.store(emptyMap())
        classHoursCache.store(null)
    }
}