package org.abgehoben.xenon.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")