package org.abgehoben.xenon.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.abgehoben.xenon.data.model.calendar.ProcessedEvent
import org.abgehoben.xenon.data.remote.SchulmanagerApi
import org.abgehoben.xenon.data.remote.dto.calendar.CalendarCategory
import org.abgehoben.xenon.data.remote.dto.calendar.CalendarEvent
import org.abgehoben.xenon.data.remote.dto.calendar.CalendarResponse
import org.abgehoben.xenon.data.remote.dto.rpc.ApiCallRequest
import org.abgehoben.xenon.data.repository.cache.CalendarCache
import org.abgehoben.xenon.data.repository.util.DateTimeParser
import java.time.LocalDate

class CalendarRepository(
    private val api: SchulmanagerApi
) {
    companion object {
        private const val TAG = "CalendarRepo"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val memoryCache = CalendarCache()

    fun clearCache() = memoryCache.clear()

    fun getCachedDaysCount(): Int = memoryCache.getCachedDaysCount()

    suspend fun getCalendarResponse(token: String, forceRefresh: Boolean = false): CalendarResponse {
        if (!forceRefresh) {
            val cached = memoryCache.getRawResponse()
            if (cached != null) return cached
        }

        val today = LocalDate.now()
        val startStr = if (today.monthValue >= 8) today.withMonth(7).withDayOfMonth(1) else today.minusYears(1).withMonth(7).withDayOfMonth(1)
        val endStr = if (today.monthValue >= 8) today.plusYears(1).withMonth(7).withDayOfMonth(31) else today.withMonth(7).withDayOfMonth(31)

        val requests = listOf(
            ApiCallRequest(
                moduleName = "calendar",
                endpointName = "get-events-for-user",
                parameters = buildJsonObject {
                    put("start", startStr.format(DateTimeParser.ISO_DATE_FORMATTER))
                    put("end", endStr.format(DateTimeParser.ISO_DATE_FORMATTER))
                    put("includeHolidays", true)
                }
            )
        )

        val response = api.fetchCallsChunked(token, requests, chunkSize = 1)
        val rawData = response.results.firstOrNull()?.data ?: JsonNull
        val calData = if (rawData !is JsonNull) {
            json.decodeFromJsonElement<CalendarResponse>(rawData)
        } else {
            CalendarResponse()
        }

        memoryCache.putRawResponse(calData)
        return calData
    }

    suspend fun getCalendarEvents(token: String, forceRefresh: Boolean = false): Map<LocalDate, List<ProcessedEvent>> {
        if (!forceRefresh) {
            val cached = memoryCache.getEvents()
            if (cached != null) return cached
        }

        val today = LocalDate.now()
        val startStr = if (today.monthValue >= 8) today.withMonth(7).withDayOfMonth(1) else today.minusYears(1).withMonth(7).withDayOfMonth(1)
        val endStr = if (today.monthValue >= 8) today.plusYears(1).withMonth(7).withDayOfMonth(31) else today.withMonth(7).withDayOfMonth(31)

        val requests = listOf(
            ApiCallRequest(
                moduleName = "calendar",
                endpointName = "get-events-for-user",
                parameters = buildJsonObject {
                    put("start", startStr.format(DateTimeParser.ISO_DATE_FORMATTER))
                    put("end", endStr.format(DateTimeParser.ISO_DATE_FORMATTER))
                    put("includeHolidays", true)
                }
            ),
            ApiCallRequest("calendar", "get-event-categories", buildJsonObject {})
        )

        try {
            val response = api.fetchCallsChunked(token, requests, chunkSize = 2)

            val eventsMap = withContext(Dispatchers.Default) {
                val calData = json.decodeFromJsonElement<CalendarResponse>(response.results.getOrNull(0)?.data ?: JsonNull)
                memoryCache.putRawResponse(calData)

                val catsRaw = json.decodeFromJsonElement<List<CalendarCategory>>(response.results.getOrNull(1)?.data ?: JsonNull)
                val catMap = catsRaw.associate { it.id to it.name }

                val allEvents = mutableListOf<CalendarEvent>()
                allEvents.addAll(calData.nonRecurringEvents ?: emptyList())
                allEvents.addAll(calData.recurringEvents ?: emptyList())
                allEvents.addAll(calData.holidays ?: emptyList())

                val eventsByDay = mutableMapOf<LocalDate, MutableList<ProcessedEvent>>()
                allEvents.forEach { ev ->
                    val startDt = DateTimeParser.parseIsoLocal(ev.start ?: ev.startDate)
                    val endDt = DateTimeParser.parseIsoLocal(ev.end ?: ev.endDate ?: ev.start)

                    if (startDt != null) {
                        val sDate = startDt.toLocalDate()
                        var eDate = endDt?.toLocalDate() ?: sDate
                        if (endDt != null && endDt.toLocalTime().isBefore(java.time.LocalTime.of(1, 0)) && eDate.isAfter(sDate)) {
                            eDate = eDate.minusDays(1)
                        }

                        val processed = ProcessedEvent(
                            title = ev.summary ?: ev.title ?: ev.name ?: "Termin",
                            description = ev.description ?: "",
                            location = ev.location ?: "",
                            organizer = ev.organizer ?: "",
                            category = catMap[ev.categoryId] ?: "Allgemein",
                            allDay = ev.allDay || (startDt.toLocalTime().isBefore(java.time.LocalTime.of(1, 0)) && (endDt == null || endDt.toLocalTime().isAfter(java.time.LocalTime.of(23, 0)))),
                            isHoliday = (ev.summary ?: ev.title ?: "").lowercase().let { it.contains("ferien") || it.contains("feiertag") },
                            startTime = startDt.toLocalTime().toString().substring(0, 5),
                            endTime = endDt?.toLocalTime()?.toString()?.substring(0, 5) ?: "",
                            startDate = sDate,
                            endDate = eDate
                        )

                        var current = sDate
                        while (!current.isAfter(eDate)) {
                            eventsByDay.getOrPut(current) { mutableListOf() }.add(processed)
                            current = current.plusDays(1)
                        }
                    }
                }
                eventsByDay
            }

            memoryCache.putEvents(eventsMap)
            return eventsMap
        } catch (e: Throwable) {
            val cached = memoryCache.getEvents()
            if (cached != null) {
                Log.w(TAG, "Network failed, serving cached calendar events")
                return cached
            }
            throw e
        }
    }

    suspend fun getIcalUrl(token: String, renew: Boolean = false): String? {

        val request = ApiCallRequest(
            moduleName = "calendar",
            endpointName = "get-ical-token",
            parameters = buildJsonObject { put("renew", renew) }
        )

        return try {
            val response = api.fetchCallsChunked(token, listOf(request), chunkSize = 1)
            val data = response.results.firstOrNull()?.data

            val icalToken = when {
                data is JsonPrimitive && data.isString -> data.content
                data is JsonObject && data["token"] != null -> data["token"]?.jsonPrimitive?.contentOrNull
                else -> null
            }

            if (!icalToken.isNullOrEmpty()) {
                "https://login.schulmanager-online.de/ical/calendar/$icalToken"
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch iCal token", e)
            null
        }
    }
}