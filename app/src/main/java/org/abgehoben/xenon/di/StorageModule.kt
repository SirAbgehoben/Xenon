package org.abgehoben.xenon.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.abgehoben.xenon.data.local.SessionManager
import org.abgehoben.xenon.data.local.SettingsManager
import org.abgehoben.xenon.data.local.datastore.sessionDataStore
import org.abgehoben.xenon.data.local.datastore.settingsDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val storageModule = module {
    single<DataStore<Preferences>>(named("sessionDataStore")) { androidContext().sessionDataStore }
    single<DataStore<Preferences>>(named("settingsDataStore")) { androidContext().settingsDataStore }

    single { SessionManager(dataStore = get(named("sessionDataStore"))) }
    single { SettingsManager(dataStore = get(named("settingsDataStore"))) }
}