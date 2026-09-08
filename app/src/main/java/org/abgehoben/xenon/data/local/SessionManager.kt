package org.abgehoben.xenon.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.abgehoben.xenon.data.local.datastore.dataStore

class SessionManager(private val context: Context) {
    companion object {
        private val JWT_TOKEN = stringPreferencesKey("jwt_token")
    }

    val jwtToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[JWT_TOKEN]
    }

    suspend fun saveJwtToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[JWT_TOKEN] = token
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(JWT_TOKEN)
        }
    }
}