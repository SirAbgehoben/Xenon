package org.abgehoben.xenon.data.repository.util

import android.util.Log
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.*
import org.abgehoben.xenon.data.local.SessionManager
import org.abgehoben.xenon.data.model.auth.UserRole
import org.abgehoben.xenon.data.remote.SchulmanagerApi

class StudentResolver(
    private val api: SchulmanagerApi,
    private val sessionManager: SessionManager,
    private val json: Json
) {
    companion object {
        private const val TAG = "StudentResolver"
    }


    /**
     * Resolves the account role, extracts all student candidates, and saves
     * the active student and role to SessionManager.
     */
    suspend fun resolveAndSync(token: String): JsonObject? {
        return try {
            val statusObj = api.getLoginStatus(token)
            val userObj = statusObj?.get("user") as? JsonObject ?: return null

            // 1. Resolve and persist UserRole
            val role = determineRole(userObj)
            sessionManager.saveUserRole(role)

            // 2. Extract student candidates
            val candidates = extractAllStudents(userObj)
            val activeStudent = candidates.firstOrNull()

            // 3. Persist active student if resolved
            if (activeStudent != null) {
                sessionManager.saveStudentData(activeStudent.toString())
                Log.d(TAG, "Resolved account: role=$role, studentId=${activeStudent["id"]}")
            }

            activeStudent
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve account from /api/login-status", e)
            null
        }
    }

    /**
     * Resolves the active student payload for timetable requests (checks local cache first).
     */
    suspend fun resolveActiveStudent(token: String): JsonObject? {
        val cached = runCatching {
            sessionManager.studentData.firstOrNull()?.let { json.parseToJsonElement(it).jsonObject }
        }.getOrNull()

        if (cached != null && cached["classId"] != null) {
            return cached
        }

        return resolveAndSync(token)
    }

    /**
     * Accurately determines the user role from Schulmanager Online's schema.
     */
    fun determineRole(userObj: JsonObject): UserRole {
        // A. Student: associatedStudent is a populated object with an id
        val directStudent = userObj["associatedStudent"] as? JsonObject
        if (directStudent != null && directStudent["id"] != null) {
            return UserRole.STUDENT
        }

        // B. Teacher: associatedTeachers array is not empty or direct associatedTeacher exists
        val teachers = userObj["associatedTeachers"] as? JsonArray
        val directTeacher = userObj["associatedTeacher"] as? JsonObject
        if (!teachers.isNullOrEmpty() || directTeacher != null) {
            return UserRole.TEACHER
        }

        // C. Parent: associatedParents array contains links or plural students exist
        val parents = userObj["associatedParents"] as? JsonArray
        val pluralStudents = userObj["associatedStudents"] as? JsonArray
        val children = userObj["children"] as? JsonArray
        if (!parents.isNullOrEmpty() ||
            !pluralStudents.isNullOrEmpty() ||
            !children.isNullOrEmpty()
        ) {
            return UserRole.PARENT
        }

        // D. Fallback: User itself has a classId
        if (userObj["classId"] != null && userObj["classId"] !is JsonNull) {
            return UserRole.STUDENT
        }

        return UserRole.STUDENT
    }

    /**
     * Extracts all unique student profiles from the user payload.
     */
    fun extractAllStudents(userObj: JsonObject): List<JsonObject> {
        val students = mutableListOf<JsonObject>()

        fun addIfUnique(student: JsonObject?) {
            if (student != null && student["id"] != null) {
                if (students.none { it["id"] == student["id"] }) {
                    students.add(student)
                }
            }
        }

        // 1. Student account: direct associatedStudent
        addIfUnique(userObj["associatedStudent"] as? JsonObject)

        // 2. Direct student object
        addIfUnique(userObj["student"] as? JsonObject)

        // 3. Parent account: associatedParents[].student (handles multiple children like Fabian & Till)
        (userObj["associatedParents"] as? JsonArray)?.forEach { parentElem ->
            val parentObj = parentElem as? JsonObject ?: return@forEach
            addIfUnique(parentObj["student"] as? JsonObject)
        }

        // 4. Plural student/children arrays
        listOfNotNull(
            userObj["associatedStudents"] as? JsonArray,
            userObj["students"] as? JsonArray,
            userObj["children"] as? JsonArray
        ).forEach { arr ->
            arr.forEach { elem -> addIfUnique(elem as? JsonObject) }
        }

        // 5. Fallback: User itself is a student (has classId)
        if (students.isEmpty() && userObj["classId"] != null && userObj["classId"] !is JsonNull) {
            students.add(userObj)
        }

        return students
    }
}