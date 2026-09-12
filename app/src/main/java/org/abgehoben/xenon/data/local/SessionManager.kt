package org.abgehoben.xenon.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.abgehoben.xenon.data.model.auth.UserRole

class SessionManager(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val JWT_TOKEN = stringPreferencesKey("jwt_token")
        private val STUDENT_DATA = stringPreferencesKey("student_data")
        private val USER_ROLE = stringPreferencesKey("user_role")
    }

    val jwtToken: Flow<String?> = dataStore.data.map { it[JWT_TOKEN] }
    val studentData: Flow<String?> = dataStore.data.map { it[STUDENT_DATA] }
    val userRole: Flow<UserRole> = dataStore.data.map { prefs ->
        prefs[USER_ROLE]?.let { runCatching { UserRole.valueOf(it) }.getOrNull() } ?: UserRole.STUDENT
    }

    suspend fun saveStudentData(studentJson: String) {
        dataStore.edit { it[STUDENT_DATA] = studentJson }
    }

    suspend fun saveUserRole(role: UserRole) {
        dataStore.edit { it[USER_ROLE] = role.name }
    }

    suspend fun saveJwtToken(token: String) {
        dataStore.edit { it[JWT_TOKEN] = token }
    }

    suspend fun clearSession() {
        dataStore.edit { it.clear() }
    }
}