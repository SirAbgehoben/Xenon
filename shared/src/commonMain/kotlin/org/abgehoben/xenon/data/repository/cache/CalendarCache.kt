package org.abgehoben.xenon.data.repository.cache

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import org.abgehoben.xenon.data.model.calendar.ProcessedEvent
import org.abgehoben.xenon.data.remote.dto.calendar.CalendarResponse
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalAtomicApi::class)
class CalendarCache {
    private val cachedEvents = AtomicReference<Map<LocalDate, List<ProcessedEvent>>?>(null)
    private val cachedResponse = AtomicReference<CalendarResponse?>(null)

    fun getEvents(): Map<LocalDate, List<ProcessedEvent>>? = cachedEvents.load()

    fun putEvents(events: Map<LocalDate, List<ProcessedEvent>>) {
        cachedEvents.store(events)
    }

    fun getRawResponse(): CalendarResponse? = cachedResponse.load()

    fun putRawResponse(response: CalendarResponse) {
        cachedResponse.store(response)
    }

    fun getCachedDaysCount(): Int = cachedEvents.load()?.size ?: 0

    fun clear() {
        cachedEvents.store(null)
        cachedResponse.store(null)
    }
}