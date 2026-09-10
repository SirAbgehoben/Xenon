// main/java/org/abgehoben/xenon/data/local/SessionManager.kt
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
        private val STUDENT_DATA = stringPreferencesKey("student_data")
    }

    val jwtToken: Flow<String?> = context.dataStore.data.map { it[JWT_TOKEN] }
    val studentData: Flow<String?> = context.dataStore.data.map { it[STUDENT_DATA] }

    suspend fun saveStudentData(studentJson: String) {
        context.dataStore.edit { it[STUDENT_DATA] = studentJson }
    }

    suspend fun saveJwtToken(token: String) {
        context.dataStore.edit { it[JWT_TOKEN] = token }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}