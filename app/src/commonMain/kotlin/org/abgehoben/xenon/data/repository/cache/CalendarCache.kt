package org.abgehoben.xenon.data.repository.cache

import org.abgehoben.xenon.data.model.calendar.ProcessedEvent
import org.abgehoben.xenon.data.remote.dto.calendar.CalendarResponse
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference

class CalendarCache {
    private val cachedEvents = AtomicReference<Map<LocalDate, List<ProcessedEvent>>?>(null)
    private val cachedResponse = AtomicReference<CalendarResponse?>(null)

    fun getEvents(): Map<LocalDate, List<ProcessedEvent>>? = cachedEvents.get()

    fun putEvents(events: Map<LocalDate, List<ProcessedEvent>>) {
        cachedEvents.set(events)
    }

    fun getRawResponse(): CalendarResponse? = cachedResponse.get()

    fun putRawResponse(response: CalendarResponse) {
        cachedResponse.set(response)
    }

    fun getCachedDaysCount(): Int = cachedEvents.get()?.size ?: 0

    fun clear() {
        cachedEvents.set(null)
        cachedResponse.set(null)
    }
}