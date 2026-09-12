package org.abgehoben.xenon.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.abgehoben.xenon.data.local.SessionManager
import org.abgehoben.xenon.data.model.system.CacheStats
import org.abgehoben.xenon.data.model.timetable.TimetableGrid
import org.abgehoben.xenon.data.remote.SchulmanagerApi
import org.abgehoben.xenon.data.remote.dto.calendar.CalendarResponse
import org.abgehoben.xenon.data.remote.dto.rpc.ApiCallRequest
import org.abgehoben.xenon.data.remote.dto.timetable.ClassHour
import org.abgehoben.xenon.data.repository.builder.TimetableGridBuilder
import org.abgehoben.xenon.data.repository.cache.TimetableCache
import org.abgehoben.xenon.data.repository.util.DateTimeParser
import org.abgehoben.xenon.data.repository.util.StudentResolver
import java.time.LocalDate

class TimetableRepository(
    private val api: SchulmanagerApi,
    private val sessionManager: SessionManager,
    private val calendarRepository: CalendarRepository,
    private val studentResolver: StudentResolver,
    private val json: Json
) {
    companion object {
        private const val TAG = "TimetableRepo"
    }

    private val memoryCache = TimetableCache()

    var lastScheduleLoadDurationMs: Long? = null
        private set

    fun clearAllCache() {
        memoryCache.clear()
        calendarRepository.clearCache()
        lastScheduleLoadDurationMs = null
    }

    fun getCacheStats(): CacheStats {
        return CacheStats(
            cachedWeeksCount = memoryCache.getCachedWeeksCount(),
            cachedCalendarDaysCount = calendarRepository.getCachedDaysCount(),
            classHoursCount = memoryCache.getClassHoursCount(),
            coursesCount = 0,
            teachersCount = 0,
            roomsCount = 0
        )
    }

    suspend fun getFullTimetable(token: String, monday: LocalDate, forceRefresh: Boolean = false): TimetableGrid {
        if (!forceRefresh && memoryCache.containsGrid(monday)) {
            Log.d(TAG, "Returning cached timetable for $monday")
            return memoryCache.getGrid(monday)!!
        }

        val startTime = System.currentTimeMillis()
        val mondayStr = monday.format(DateTimeParser.ISO_DATE_FORMATTER)
        val sundayStr = monday.plusDays(6).format(DateTimeParser.ISO_DATE_FORMATTER)

        // 1. Resolve student payload
        val studentJson = studentResolver.resolveActiveStudent(token)

        // 2. Fetch vacation response from Calendar domain
        val calendar = runCatching {
            calendarRepository.getCalendarResponse(token, forceRefresh)
        }.getOrDefault(CalendarResponse())

        // 3. Prepare schedule and class hour requests
        val dynamicRequests = mutableListOf(
            ApiCallRequest(
                moduleName = "schedules",
                endpointName = "get-actual-lessons",
                parameters = buildJsonObject {
                    if (studentJson != null) put("student", studentJson)
                    put("start", mondayStr)
                    put("end", sundayStr)
                }
            )
        )

        val metadataRequests = if (memoryCache.getClassHours() == null) {
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

                if (metadataRequests.isNotEmpty() && data.size >= 2) {
                    val chList = data.getOrNull(1)?.takeIf { it !is JsonNull }?.let {
                        json.decodeFromJsonElement<List<ClassHour>>(it)
                    } ?: emptyList()

                    if (chList.isNotEmpty()) {
                        memoryCache.putClassHours(chList)
                    }
                }

                val classHours = memoryCache.getClassHours() ?: emptyList()

                TimetableGridBuilder.build(
                    monday = monday,
                    classHours = classHours,
                    actualLessonsData = actualLessonsRaw,
                    calendar = calendar
                )
            }

            memoryCache.putGrid(monday, grid)
            lastScheduleLoadDurationMs = System.currentTimeMillis() - startTime
            return grid
        } catch (e: Throwable) {
            if (memoryCache.containsGrid(monday)) {
                Log.w(TAG, "Network failed, serving cached timetable for $monday")
                return memoryCache.getGrid(monday)!!
            }
            throw e
        }
    }
}