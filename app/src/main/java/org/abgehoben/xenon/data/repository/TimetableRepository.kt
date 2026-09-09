package org.abgehoben.xenon.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.abgehoben.xenon.data.local.SessionManager
import org.abgehoben.xenon.data.model.calendar.IcalType
import org.abgehoben.xenon.data.model.calendar.ProcessedEvent
import org.abgehoben.xenon.data.model.system.CacheStats
import org.abgehoben.xenon.data.model.timetable.SchoolMetadata
import org.abgehoben.xenon.data.model.timetable.TimetableGrid
import org.abgehoben.xenon.data.remote.SchulmanagerApi
import org.abgehoben.xenon.data.remote.dto.calendar.CalendarCategory
import org.abgehoben.xenon.data.remote.dto.calendar.CalendarEvent
import org.abgehoben.xenon.data.remote.dto.calendar.CalendarResponse
import org.abgehoben.xenon.data.remote.dto.rpc.ApiCallRequest
import org.abgehoben.xenon.data.remote.dto.timetable.ClassHour
import org.abgehoben.xenon.data.repository.builder.TimetableGridBuilder
import org.abgehoben.xenon.data.repository.cache.TimetableCache
import org.abgehoben.xenon.data.repository.util.DateTimeParser
import java.time.LocalDate

class TimetableRepository(
    private val api: SchulmanagerApi,
    private val sessionManager: SessionManager? = null
) {
    companion object {
        private const val TAG = "TimetableRepo"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val memoryCache = TimetableCache()

    fun clearAllCache() = memoryCache.clear()

    fun getCacheStats(): CacheStats = memoryCache.getStats()

    suspend fun getFullTimetable(token: String, monday: LocalDate, forceRefresh: Boolean = false): TimetableGrid {
        if (!forceRefresh && memoryCache.containsGrid(monday)) {
            Log.d(TAG, "Returning cached timetable for $monday")
            return memoryCache.getGrid(monday)!!
        }

        val mondayStr = monday.format(DateTimeParser.ISO_DATE_FORMATTER)
        val sundayStr = monday.plusDays(6).format(DateTimeParser.ISO_DATE_FORMATTER)
        val calStartStr = monday.minusMonths(3).format(DateTimeParser.ISO_DATE_FORMATTER)
        val calEndStr = monday.plusMonths(2).format(DateTimeParser.ISO_DATE_FORMATTER)

        val studentJson = resolveStudentObject(token)

        val dynamicRequests = mutableListOf(
            ApiCallRequest(
                moduleName = "schedules",
                endpointName = "get-actual-lessons",
                parameters = buildJsonObject {
                    if (studentJson != null) {
                        put("student", studentJson)
                    }
                    put("start", mondayStr)
                    put("end", sundayStr)
                }
            ),
            ApiCallRequest(
                moduleName = "calendar",
                endpointName = "get-events-for-user",
                parameters = buildJsonObject {
                    put("start", calStartStr)
                    put("end", calEndStr)
                    put("includeHolidays", true)
                }
            )
        )

        val metadataRequests = if (memoryCache.getMetadata() == null) {
            listOf(
                ApiCallRequest(
                    moduleName = "main",
                    endpointName = "poqa",
                    parameters = buildJsonObject {
                        putJsonObject("action") {
                            put("model", "main/class-hour")
                            put("action", "findAll")
                            putJsonArray("parameters") { add(buildJsonObject {}) }
                        }
                    }
                )
            )
        } else emptyList()

        val allRequests = dynamicRequests + metadataRequests

        try {
            val response = api.fetchCallsChunked(token, allRequests, chunkSize = 4)
            val data = response.results.map { it.data ?: JsonNull }

            val grid = withContext(Dispatchers.Default) {
                val actualLessonsRaw = data.getOrNull(0)?.takeIf { it !is JsonNull }
                val calendar = data.getOrNull(1)?.takeIf { it !is JsonNull }?.let {
                    json.decodeFromJsonElement<CalendarResponse>(it)
                } ?: CalendarResponse()

                if (metadataRequests.isNotEmpty() && data.size >= 3) {
                    val chList = data.getOrNull(2)?.takeIf { it !is JsonNull }?.let {
                        json.decodeFromJsonElement<List<ClassHour>>(it)
                    } ?: emptyList()

                    if (chList.isNotEmpty()) {
                        memoryCache.putMetadata(
                            SchoolMetadata(
                                classHours = chList,
                                courses = emptyList(),
                                rooms = emptyList(),
                                teachers = emptyList(),
                                tca = emptyList()
                            )
                        )
                    }
                }

                val classHours = memoryCache.getMetadata()?.classHours ?: emptyList()

                TimetableGridBuilder.build(
                    monday = monday,
                    classHours = classHours,
                    actualLessonsData = actualLessonsRaw,
                    calendar = calendar
                )
            }

            memoryCache.putGrid(monday, grid)
            return grid
        } catch (e: Throwable) {
            if (memoryCache.containsGrid(monday)) {
                Log.w(TAG, "Network failed, serving cached timetable for $monday")
                return memoryCache.getGrid(monday)!!
            }
            throw e
        }
    }

    suspend fun getCalendarEvents(token: String, forceRefresh: Boolean = false): Map<LocalDate, List<ProcessedEvent>> {
        if (!forceRefresh) {
            val cached = memoryCache.getCalendarEvents()
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

            memoryCache.putCalendarEvents(eventsMap)
            return eventsMap
        } catch (e: Throwable) {
            val cached = memoryCache.getCalendarEvents()
            if (cached != null) {
                Log.w(TAG, "Network failed, serving cached calendar events")
                return cached
            }
            throw e
        }
    }

    suspend fun getIcalUrl(token: String, type: IcalType, renew: Boolean = false): String? {
        val moduleName = if (type == IcalType.TIMETABLE) "schedules" else "calendar"
        val endpointName = if (type == IcalType.TIMETABLE) "get-schedules-ical-token" else "get-ical-token"
        val urlPath = if (type == IcalType.TIMETABLE) "schedules" else "calendar"

        val request = ApiCallRequest(
            moduleName = moduleName,
            endpointName = endpointName,
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
                "https://login.schulmanager-online.de/ical/$urlPath/$icalToken"
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch iCal token for $type", e)
            null
        }
    }

    private suspend fun resolveStudentObject(token: String): JsonObject? {
        // 1. Check if we already have the valid student (with classId) cached in DataStore
        var studentJson: JsonObject? = runCatching {
            sessionManager?.studentData?.firstOrNull()?.let { json.parseToJsonElement(it).jsonObject }
        }.getOrNull()

        // If cached object is a parent account (no classId), force re-resolve
        if (studentJson != null && studentJson["classId"] != null) {
            return studentJson
        }

        // 2. Fetch login-status and resolve student from associatedParents or associatedStudent
        try {
            val statusObj = api.getLoginStatus(token)
            val userObj = statusObj?.get("user") as? JsonObject

            //TODO: Still need to implement everything for multiple childs (including a way to switch between them)

            // Case A: Student account -> user.associatedStudent
            val directStudent = userObj?.get("associatedStudent") as? JsonObject

            // Case B: Parent account -> user.associatedParents[0].student (exact path from your console output!)
            val parentStudent = (userObj?.get("associatedParents") as? JsonArray)
                ?.mapNotNull { (it as? JsonObject)?.get("student") as? JsonObject }
                ?.firstOrNull()

            // Case C: Plural array associatedStudents
            val pluralStudent = (userObj?.get("associatedStudents") as? JsonArray)
                ?.firstOrNull() as? JsonObject

            val resolved = directStudent
                ?: parentStudent
                ?: pluralStudent
                ?: userObj

            if (resolved != null) {
                studentJson = resolved
                sessionManager?.saveStudentData(resolved.toString())
                Log.d(TAG, "Successfully resolved active student dynamically: $resolved")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve student from /api/login-status", e)
        }

        return studentJson
    }
}