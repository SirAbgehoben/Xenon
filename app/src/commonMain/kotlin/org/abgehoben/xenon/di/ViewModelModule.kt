package org.abgehoben.xenon.di

import org.abgehoben.xenon.MainViewModel
import org.abgehoben.xenon.ui.screens.calendar.CalendarViewModel
import org.abgehoben.xenon.ui.screens.settings.SettingsViewModel
import org.abgehoben.xenon.ui.screens.timetable.TimetableViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::TimetableViewModel)
    viewModelOf(::CalendarViewModel)
    viewModelOf(::SettingsViewModel)
}