package org.abgehoben.xenon.data

import androidx.datastore.core.DataStore
import java.util.prefs.Preferences
import org.abgehoben.xenon.data.local.datastore.dataStore as internalDataStore

//TODO: this is just a temporary file used while refactoring since I did not want to adjust EVERYTHING ELSE to use this canonical design

typealias ThemeMode = org.abgehoben.xenon.data.local.model.ThemeMode
typealias UserSettings = org.abgehoben.xenon.data.local.model.UserSettings
typealias SessionManager = org.abgehoben.xenon.data.local.SessionManager
typealias SettingsManager = org.abgehoben.xenon.data.local.SettingsManager

typealias SchulmanagerApi = org.abgehoben.xenon.data.remote.SchulmanagerApi
typealias ApiCallRequest = org.abgehoben.xenon.data.remote.dto.rpc.ApiCallRequest
typealias ApiCallBundle = org.abgehoben.xenon.data.remote.dto.rpc.ApiCallBundle
typealias ApiCallResult = org.abgehoben.xenon.data.remote.dto.rpc.ApiCallResult
typealias ApiCallResponse = org.abgehoben.xenon.data.remote.dto.rpc.ApiCallResponse

typealias LoginResponse = org.abgehoben.xenon.data.remote.dto.auth.LoginResponse
typealias User = org.abgehoben.xenon.data.remote.dto.auth.User
typealias Account = org.abgehoben.xenon.data.remote.dto.auth.Account

typealias ClassHour = org.abgehoben.xenon.data.remote.dto.timetable.ClassHour
typealias Course = org.abgehoben.xenon.data.remote.dto.timetable.Course
typealias Subject = org.abgehoben.xenon.data.remote.dto.timetable.Subject
typealias Lesson = org.abgehoben.xenon.data.remote.dto.timetable.Lesson
typealias Room = org.abgehoben.xenon.data.remote.dto.timetable.Room
typealias Teacher = org.abgehoben.xenon.data.remote.dto.timetable.Teacher
typealias TeacherCourseAttendance = org.abgehoben.xenon.data.remote.dto.timetable.TeacherCourseAttendance
typealias Substitution = org.abgehoben.xenon.data.remote.dto.timetable.Substitution

typealias CalendarEvent = org.abgehoben.xenon.data.remote.dto.calendar.CalendarEvent
typealias CalendarResponse = org.abgehoben.xenon.data.remote.dto.calendar.CalendarResponse
typealias CalendarCategory = org.abgehoben.xenon.data.remote.dto.calendar.CalendarCategory

typealias TimetableSlot = org.abgehoben.xenon.data.model.timetable.TimetableSlot
typealias TimetableGrid = org.abgehoben.xenon.data.model.timetable.TimetableGrid
typealias MergedSlot = org.abgehoben.xenon.data.model.timetable.MergedSlot
typealias SubstitutionSummary = org.abgehoben.xenon.data.model.timetable.SubstitutionSummary
typealias SchoolMetadata = org.abgehoben.xenon.data.model.timetable.SchoolMetadata

typealias ProcessedEvent = org.abgehoben.xenon.data.model.calendar.ProcessedEvent
typealias CacheStats = org.abgehoben.xenon.data.model.system.CacheStats

typealias TimetableRepository = org.abgehoben.xenon.data.repository.TimetableRepository
typealias TimetableGridBuilder = org.abgehoben.xenon.data.repository.builder.TimetableGridBuilder

val android.content.Context.dataStore: DataStore<androidx.datastore.preferences.core.Preferences>
    get() = this.internalDataStore