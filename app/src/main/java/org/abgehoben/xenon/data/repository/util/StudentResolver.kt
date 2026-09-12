package org.abgehoben.xenon.data.repository.util

import android.util.Log
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.*
import org.abgehoben.xenon.data.local.SessionManager
import org.abgehoben.xenon.data.remote.SchulmanagerApi

class StudentResolver(
    private val api: SchulmanagerApi,
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "StudentResolver"
    }

    private val json = Json { ignoreUnknownKeys = true }

    //TODO: this still just searches for one student, there is the possibility of a parent having multiple :surprised_pikachu:, I still need to implement that, including an option in the settings to switch between them (at least for now I think that would be the best way to handle that)
    suspend fun resolveActiveStudent(token: String): JsonObject? {
        // 1. Check the local session cache
        val cachedJson = runCatching {
            sessionManager.studentData.firstOrNull()?.let { json.parseToJsonElement(it).jsonObject }
        }.getOrNull()

        if (cachedJson != null && cachedJson["classId"] != null) {
            return cachedJson
        }

        // 2. Query /api/login-status and resolve candidate
        try {
            val statusObj = api.getLoginStatus(token)
            val userObj = statusObj?.get("user") as? JsonObject
            val candidates = extractAllStudents(userObj)

            val resolved = candidates.firstOrNull()
            if (resolved != null) {
                sessionManager.saveStudentData(resolved.toString())
                Log.d(TAG, "Resolved active student dynamically: $resolved")
                return resolved
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve student from /api/login-status", e)
        }

        return cachedJson
    }

    private fun extractAllStudents(userObj: JsonObject?): List<JsonObject> {
        if (userObj == null) return emptyList()
        val students = mutableListOf<JsonObject>()

        // A. Student account: direct associatedStudent
        (userObj["associatedStudent"] as? JsonObject)?.let {
            if (it["id"] != null) students.add(it)
        }

        // B. Direct student field
        (userObj["student"] as? JsonObject)?.let {
            if (it["id"] != null) students.add(it)
        }

        // C. Parent account: associatedParents[].student
        (userObj["associatedParents"] as? JsonArray)?.forEach { parentElem ->
            val parentObj = parentElem as? JsonObject ?: return@forEach
            (parentObj["student"] as? JsonObject)?.let { student ->
                if (student["id"] != null && students.none { it["id"] == student["id"] }) {
                    students.add(student)
                }
            }
        }

        // D. Plural student arrays
        listOfNotNull(
            userObj["associatedStudents"] as? JsonArray,
            userObj["students"] as? JsonArray,
            userObj["children"] as? JsonArray
        ).forEach { arr ->
            arr.forEach { elem ->
                (elem as? JsonObject)?.let { student ->
                    if (student["id"] != null && students.none { it["id"] == student["id"] }) {
                        students.add(student)
                    }
                }
            }
        }

        // E. Fallback: User itself is a student (has classId)
        if (students.isEmpty() && userObj["classId"] != null) {
            students.add(userObj)
        }

        return students
    }
}