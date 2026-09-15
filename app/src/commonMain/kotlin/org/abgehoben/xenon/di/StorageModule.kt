package org.abgehoben.xenon.di

import org.abgehoben.xenon.data.local.SessionManager
import org.abgehoben.xenon.data.local.SettingsManager
import org.koin.core.qualifier.named
import org.koin.dsl.module

val storageModule = module {
    single { SessionManager(dataStore = get(named("sessionDataStore"))) }
    single { SettingsManager(dataStore = get(named("settingsDataStore"))) }
}