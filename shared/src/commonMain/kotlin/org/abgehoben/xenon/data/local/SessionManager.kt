package org.abgehoben.xenon.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.abgehoben.xenon.data.model.auth.UserRole
import kotlin.concurrent.Volatile

class SessionManager(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val JWT_TOKEN = stringPreferencesKey("jwt_token")
        private val STUDENT_DATA = stringPreferencesKey("student_data")
        private val USER_ROLE = stringPreferencesKey("user_role")
    }

    @Volatile
    var cachedToken: String? = null
        private set

    val jwtToken: Flow<String?> = dataStore.data
        .map { it[JWT_TOKEN] }
        .distinctUntilChanged()
        .onEach { cachedToken = it }

    val studentData: Flow<String?> = dataStore.data
        .map { it[STUDENT_DATA] }
        .distinctUntilChanged()

    val userRole: Flow<UserRole> = dataStore.data
        .map { prefs ->
            prefs[USER_ROLE]?.let { runCatching { UserRole.valueOf(it) }.getOrNull() } ?: UserRole.STUDENT
        }
        .distinctUntilChanged()

    suspend fun saveStudentData(studentJson: String) {
        dataStore.edit { it[STUDENT_DATA] = studentJson }
    }

    suspend fun saveUserRole(role: UserRole) {
        dataStore.edit { it[USER_ROLE] = role.name }
    }

    suspend fun saveJwtToken(token: String) {
        cachedToken = token
        dataStore.edit { it[JWT_TOKEN] = token }
    }

    suspend fun clearSession() {
        cachedToken = null
        dataStore.edit { it.clear() }
    }
}