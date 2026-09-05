package org.abgehoben.xenon.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LoginResponse(
    val jwt: String? = null,
    val requireTwoFactorEmailCode: Boolean = false,
    val requireTOTP: Boolean = false,
    val multipleAccounts: List<Account>? = null,
    val userId: Int? = null,
    val user: User? = null
)

@Serializable
data class User(
    val id: Int? = null,
    val firstname: String? = null,
    val lastname: String? = null
)

@Serializable
data class Account(
    val userId: Int,
    val firstname: String,
    val lastname: String,
    val institutionName: String
)

@Serializable
data class ApiCallRequest(
    val moduleName: String,
    val endpointName: String,
    val parameters: JsonElement
)

@Serializable
data class ApiCallBundle(
    val bundleVersion: String = "deadbeef00",
    val requests: List<ApiCallRequest>
)

@Serializable
data class ApiCallResult(
    val status: Int,
    val data: JsonElement? = null
)

@Serializable
data class ApiCallResponse(
    val results: List<ApiCallResult>
)

@Serializable
data class ClassHour(
    val id: Int,
    val number: Int,
    val from: String,
    val until: String
)

@Serializable
data class Course(
    val id: Int,
    val name: String? = null,
    val abbreviation: String? = null,
    val subject: Subject? = null,
    val teacher: Teacher? = null,
    val teachers: List<Teacher>? = null
)

@Serializable
data class Subject(
    val abbreviation: String? = null
)

@Serializable
data class Lesson(
    val id: Int,
    val dayOfWeek: Int,
    val classHourId: Int,
    val courseId: Int,
    val roomId: Int? = null,
    val occurrenceId: Int? = null,
    val start: String? = null,
    val end: String? = null
)

@Serializable
data class Room(
    val id: Int,
    val name: String? = null,
    val abbreviation: String? = null
)

@Serializable
data class Teacher(
    val id: Int,
    val abbreviation: String? = null,
    val lastname: String? = null
)

@Serializable
data class TeacherCourseAttendance(
    val courseId: Int,
    val teacherId: Int
)

@Serializable
data class Substitution(
    val id: Int,
    val date: String,
    val lessonId: Int? = null,
    val courseId: Int? = null,
    val roomId: Int? = null,
    val classHourId: Int? = null,
    val cancelled: Boolean? = null,
    val comment: String? = null,
    val teachers: List<Teacher>? = null,
    val course: Course? = null,
    val room: Room? = null,
    // Add this line below to catch the lessons list from the API:
    val lessons: List<Lesson>? = null
)

@Serializable
data class CalendarEvent(
    val summary: String? = null,
    val title: String? = null,
    val name: String? = null,
    val start: String? = null,
    val end: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val allDay: Boolean = false,
    val description: String? = null,
    val location: String? = null,
    val organizer: String? = null,
    val categoryId: Int? = null
)

@Serializable
data class CalendarResponse(
    val nonRecurringEvents: List<CalendarEvent>? = null,
    val recurringEvents: List<CalendarEvent>? = null,
    val holidays: List<CalendarEvent>? = null
)

@Serializable
data class CalendarCategory(
    val id: Int,
    val name: String
)
