package org.abgehoben.xenon.data

import androidx.datastore.core.DataStore
import org.abgehoben.xenon.data.local.datastore.dataStore as internalDataStore

//TODO: this is just a temporary file used while refactoring since I did not want to adjust EVERYTHING ELSE to use this canonical design

typealias ThemeMode = org.abgehoben.xenon.data.local.model.ThemeMode
typealias UserSettings = org.abgehoben.xenon.data.local.model.UserSettings
typealias ApiCallRequest = org.abgehoben.xenon.data.remote.dto.rpc.ApiCallRequest

typealias ClassHour = org.abgehoben.xenon.data.remote.dto.timetable.ClassHour

typealias TimetableSlot = org.abgehoben.xenon.data.model.timetable.TimetableSlot
typealias TimetableGrid = org.abgehoben.xenon.data.model.timetable.TimetableGrid
typealias MergedSlot = org.abgehoben.xenon.data.model.timetable.MergedSlot

typealias ProcessedEvent = org.abgehoben.xenon.data.model.calendar.ProcessedEvent
typealias CacheStats = org.abgehoben.xenon.data.model.system.CacheStats

val android.content.Context.dataStore: DataStore<androidx.datastore.preferences.core.Preferences>
    get() = this.internalDataStore