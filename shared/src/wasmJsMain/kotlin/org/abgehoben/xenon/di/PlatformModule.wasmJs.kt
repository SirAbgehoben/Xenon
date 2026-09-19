package org.abgehoben.xenon.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.abgehoben.xenon.platform.PlatformInfo
import org.abgehoben.xenon.platform.PlatformNotifier
import org.abgehoben.xenon.platform.WasmPlatformInfo
import org.abgehoben.xenon.platform.WasmPlatformNotifier
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<HttpClientEngine> { Js.create() }

    single<DataStore<Preferences>>(named("sessionDataStore")) {
        WasmPreferencesDataStore("sessionDataStore")
    }

    single<DataStore<Preferences>>(named("settingsDataStore")) {
        WasmPreferencesDataStore("settingsDataStore")
    }

    single<PlatformInfo> { WasmPlatformInfo() }
    single<PlatformNotifier> { WasmPlatformNotifier() }
}

/**
 * WebAssembly-compatible DataStore backed by browser localStorage.
 * Resolves the NotImplementedError thrown by PreferenceDataStoreFactory.createWithPath on wasmJs.
 */
private class WasmPreferencesDataStore(
    private val storeName: String
) : DataStore<Preferences> {

    private val prefix = "$storeName::"
    private val _data = MutableStateFlow(loadFromStorage())

    override val data: Flow<Preferences> = _data.asStateFlow()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val current = _data.value
        val updated = transform(current)
        _data.value = updated
        saveToStorage(updated)
        return updated
    }

    private fun loadFromStorage(): Preferences {
        val mutable = mutablePreferencesOf()
        val storage = runCatching { window.localStorage }.getOrNull() ?: return mutable

        for (i in 0 until storage.length) {
            val fullKey = storage.key(i) ?: continue
            if (fullKey.startsWith(prefix)) {
                val keyName = fullKey.removePrefix(prefix)
                val rawValue = storage.getItem(fullKey) ?: continue
                when {
                    rawValue.startsWith("b:") -> mutable[booleanPreferencesKey(keyName)] = rawValue.substring(2).toBoolean()
                    rawValue.startsWith("i:") -> rawValue.substring(2).toIntOrNull()?.let { mutable[intPreferencesKey(keyName)] = it }
                    rawValue.startsWith("l:") -> rawValue.substring(2).toLongOrNull()?.let { mutable[longPreferencesKey(keyName)] = it }
                    rawValue.startsWith("s:") -> mutable[stringPreferencesKey(keyName)] = rawValue.substring(2)
                    else -> mutable[stringPreferencesKey(keyName)] = rawValue
                }
            }
        }
        return mutable.toPreferences()
    }

    private fun saveToStorage(prefs: Preferences) {
        val storage = runCatching { window.localStorage }.getOrNull() ?: return

        // Clear existing keys for this specific store
        val keysToRemove = mutableListOf<String>()
        for (i in 0 until storage.length) {
            val k = storage.key(i) ?: continue
            if (k.startsWith(prefix)) {
                keysToRemove.add(k)
            }
        }
        keysToRemove.forEach { storage.removeItem(it) }

        // Persist typed keys
        for ((key, value) in prefs.asMap()) {
            val encoded = when (value) {
                is Boolean -> "b:$value"
                is Int -> "i:$value"
                is Long -> "l:$value"
                is String -> "s:$value"
                else -> "s:$value"
            }
            storage.setItem("$prefix${key.name}", encoded)
        }
    }
}