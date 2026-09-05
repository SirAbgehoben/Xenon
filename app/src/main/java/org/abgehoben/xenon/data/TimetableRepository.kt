package org.abgehoben.xenon.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TimetableRepository(private val api: SchulmanagerApi) {
    companion object {
        private const val TAG = "TimetableRepo"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val timetableCache = mutableMapOf<LocalDate, TimetableGrid>()
    private var cachedMetadata: SchoolMetadata? = null

    suspend fun getFullTimetable(token: String, monday: LocalDate, forceRefresh: Boolean = false): TimetableGrid {
        if (forceRefresh) {
            timetableCache.clear()
            cachedMetadata = null
        } else if (timetableCache.containsKey(monday)) {
            Log.d(TAG, "Returning cached timetable for $monday")
            return timetableCache[monday]!!
        }

        val mondayStr = monday.format(dateFormatter)
        val fridayStr = monday.plusDays(4).format(dateFormatter)
        val calStartStr = monday.minusMonths(3).format(dateFormatter)
        val calEndStr = monday.plusMonths(2).format(dateFormatter)

        val dynamicRequests = mutableListOf(
            ApiCallRequest("main", "poqa", buildJsonObject {
                putJsonObject("action") {
                    put("model", "main/lesson")
                    put("action", "findAll")
                    putJsonArray("parameters") {
                        add(buildJsonObject {
                            putJsonObject("where") {
                                putJsonObject("start") { put("\$lte", fridayStr) }
                                putJsonObject("end") { put("\$gte", mondayStr) }
                            }
                        })
                    }
                }
            }),
            ApiCallRequest("main", "poqa", buildJsonObject {
                putJsonObject("action") {
                    put("model", "main/substitution")
                    put("action", "findAll")
                    putJsonArray("parameters") {
                        add(buildJsonObject {
                            putJsonObject("where") {
                                putJsonObject("date") {
                                    put("\$gte", mondayStr)
                                    put("\$lte", fridayStr)
                                }
                            }
                            putJsonArray("include") {
                                add(buildJsonObject { put("association", "course"); put("required", false) })
                                add(buildJsonObject { put("association", "room"); put("required", false) })
                                add(buildJsonObject { put("association", "teachers"); put("required", false) })
                                add(buildJsonObject { put("association", "lessons"); put("required", false) })
                            }
                        })
                    }
                }
            }),
            ApiCallRequest("calendar", "get-events-for-user", buildJsonObject {
                put("start", calStartStr)
                put("end", calEndStr)
                put("includeHolidays", true)
            })
        )

        val metadataRequests = if (cachedMetadata == null) {
            listOf(
                ApiCallRequest("main", "poqa", buildJsonObject {
                    putJsonObject("action") { put("model", "main/class-hour"); put("action", "findAll"); putJsonArray("parameters") { add(buildJsonObject {}) } }
                }),
                ApiCallRequest("main", "poqa", buildJsonObject {
                    putJsonObject("action") { put("model", "main/course"); put("action", "findAll"); putJsonArray("parameters") { add(buildJsonObject {}) } }
                }),
                ApiCallRequest("main", "poqa", buildJsonObject {
                    putJsonObject("action") { put("model", "main/room"); put("action", "findAll"); putJsonArray("parameters") { add(buildJsonObject {}) } }
                }),
                ApiCallRequest("main", "poqa", buildJsonObject {
                    putJsonObject("action") { put("model", "main/teacher"); put("action", "findAll"); putJsonArray("parameters") { add(buildJsonObject {}) } }
                }),
                ApiCallRequest("main", "poqa", buildJsonObject {
                    putJsonObject("action") { put("model", "main/teacher-course-attendance"); put("action", "findAll"); putJsonArray("parameters") { add(buildJsonObject {}) } }
                })
            )
        } else emptyList()

        val allRequests = dynamicRequests + metadataRequests
        val response = api.fetchCallsChunked(token, allRequests, chunkSize = 4)
        val data = response.results.map { it.data ?: JsonNull }

        val grid = withContext(Dispatchers.Default) {
            val lessons = data.getOrNull(0)?.takeIf { it !is JsonNull }?.let { json.decodeFromJsonElement<List<Lesson>>(it) } ?: emptyList()
            val substitutions = data.getOrNull(1)?.takeIf { it !is JsonNull }?.let { json.decodeFromJsonElement<List<Substitution>>(it) } ?: emptyList()
            val calendar = data.getOrNull(2)?.takeIf { it !is JsonNull }?.let { json.decodeFromJsonElement<CalendarResponse>(it) } ?: CalendarResponse()

            if (metadataRequests.isNotEmpty() && data.size >= 8) {
                val chList = data.getOrNull(3)?.takeIf { it !is JsonNull }?.let { json.decodeFromJsonElement<List<ClassHour>>(it) } ?: emptyList()
                val cList = data.getOrNull(4)?.takeIf { it !is JsonNull }?.let { json.decodeFromJsonElement<List<Course>>(it) } ?: emptyList()

                if (chList.isNotEmpty() && cList.isNotEmpty()) {
                    cachedMetadata = SchoolMetadata(
                        classHours = chList,
                        courses = cList,
                        rooms = data.getOrNull(5)?.takeIf { it !is JsonNull }?.let { json.decodeFromJsonElement(it) } ?: emptyList(),
                        teachers = data.getOrNull(6)?.takeIf { it !is JsonNull }?.let { json.decodeFromJsonElement(it) } ?: emptyList(),
                        tca = data.getOrNull(7)?.takeIf { it !is JsonNull }?.let { json.decodeFromJsonElement(it) } ?: emptyList()
                    )
                }
            }

            val meta = cachedMetadata ?: SchoolMetadata(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

            TimetableGridBuilder.build(
                monday = monday,
                classHours = meta.classHours,
                coursesRaw = meta.courses,
                lessons = lessons,
                rooms = meta.rooms,
                teachers = meta.teachers,
                tca = meta.tca,
                subsRaw = substitutions,
                calendar = calendar
            )
        }

        timetableCache[monday] = grid
        return grid
    }

    suspend fun getCalendarEvents(token: String): Map<LocalDate, List<ProcessedEvent>> {
        val today = LocalDate.now()
        val startStr = if (today.monthValue >= 8) today.withMonth(7).withDayOfMonth(1) else today.minusYears(1).withMonth(7).withDayOfMonth(1)
        val endStr = if (today.monthValue >= 8) today.plusYears(1).withMonth(7).withDayOfMonth(31) else today.withMonth(7).withDayOfMonth(31)

        val requests = listOf(
            ApiCallRequest("calendar", "get-events-for-user", buildJsonObject {
                put("start", startStr.format(dateFormatter))
                put("end", endStr.format(dateFormatter))
                put("includeHolidays", true)
            }),
            ApiCallRequest("calendar", "get-event-categories", buildJsonObject {})
        )

        val response = api.fetchCallsChunked(token, requests, chunkSize = 2)

        return withContext(Dispatchers.Default) {
            val calData = json.decodeFromJsonElement<CalendarResponse>(response.results.getOrNull(0)?.data ?: JsonNull)
            val catsRaw = json.decodeFromJsonElement<List<CalendarCategory>>(response.results.getOrNull(1)?.data ?: JsonNull)
            val catMap = catsRaw.associate { it.id to it.name }

            val allEvents = mutableListOf<CalendarEvent>()
            allEvents.addAll(calData.nonRecurringEvents ?: emptyList())
            allEvents.addAll(calData.recurringEvents ?: emptyList())
            allEvents.addAll(calData.holidays ?: emptyList())

            val eventsByDay = mutableMapOf<LocalDate, MutableList<ProcessedEvent>>()
            allEvents.forEach { ev ->
                val startDt = TimetableGridBuilder.parseIsoLocal(ev.start ?: ev.startDate ?: "")
                val endDt = TimetableGridBuilder.parseIsoLocal(ev.end ?: ev.endDate ?: ev.start ?: "")

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
    }
}