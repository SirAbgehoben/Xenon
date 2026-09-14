package org.abgehoben.xenon.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.abgehoben.xenon.data.local.SessionManager
import org.abgehoben.xenon.data.local.SettingsManager
import org.abgehoben.xenon.platform.createPlatformDataStore
import org.koin.core.qualifier.named
import org.koin.dsl.module

val storageModule = module {
    single<DataStore<Preferences>>(named("sessionDataStore")) {
        createPlatformDataStore("session.preferences_pb")
    }
    single<DataStore<Preferences>>(named("settingsDataStore")) {
        createPlatformDataStore("settings.preferences_pb")
    }

    single { SessionManager(dataStore = get(named("sessionDataStore"))) }
    single { SettingsManager(dataStore = get(named("settingsDataStore"))) }
}