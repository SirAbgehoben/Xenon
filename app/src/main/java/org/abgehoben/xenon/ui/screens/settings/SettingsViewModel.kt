package org.abgehoben.xenon.ui.screens.settings

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import org.abgehoben.xenon.data.local.SessionManager
import org.abgehoben.xenon.data.local.SettingsManager
import org.abgehoben.xenon.data.local.model.ThemeMode
import org.abgehoben.xenon.data.local.model.UserSettings
import org.abgehoben.xenon.data.model.auth.UserRole
import org.abgehoben.xenon.data.model.system.CacheStats
import org.abgehoben.xenon.data.model.timetable.TimetableViewMode
import org.abgehoben.xenon.data.remote.SchulmanagerApi
import org.abgehoben.xenon.data.remote.dto.rpc.ApiCallRequest
import org.abgehoben.xenon.data.repository.CalendarRepository
import org.abgehoben.xenon.data.repository.TimetableRepository

class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val sessionManager: SessionManager,
    private val timetableRepository: TimetableRepository,
    private val calendarRepository: CalendarRepository,
    private val api: SchulmanagerApi,
    application: Application
) : AndroidViewModel(application) {

    val userSettings: StateFlow<UserSettings> = settingsManager.userSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val jwtToken: StateFlow<String?> = sessionManager.jwtToken
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userRole: StateFlow<UserRole> = sessionManager.userRole
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserRole.STUDENT)

    val lastScheduleLoadDurationMs: Long?
        get() = timetableRepository.lastScheduleLoadDurationMs

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsManager.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsManager.setDynamicColor(enabled) }
    fun setDefaultViewMode(mode: TimetableViewMode) = viewModelScope.launch { settingsManager.setDefaultViewMode(mode) }
    fun setMergeLessons(enabled: Boolean) = viewModelScope.launch { settingsManager.setMergeLessons(enabled) }
    fun setWeekendAdvance(enabled: Boolean) = viewModelScope.launch { settingsManager.setWeekendAdvance(enabled) }
    fun setScaleBreaks(enabled: Boolean) = viewModelScope.launch { settingsManager.setScaleBreaks(enabled) }
    fun setPreloadWeeks(enabled: Boolean) = viewModelScope.launch { settingsManager.setPreloadWeeks(enabled) }

    fun clearAppCache() {
        timetableRepository.clearAllCache()
    }

    suspend fun fetchIcalUrl(renew: Boolean = false): String? {
        val token = jwtToken.value ?: return null
        return calendarRepository.getIcalUrl(token, renew)
    }

    fun getTimetableCacheStats(): CacheStats = timetableRepository.getCacheStats()

    suspend fun pingServer(): Pair<Boolean, Long> {
        val start = System.currentTimeMillis()
        return try {
            val response = api.fetchCallsChunked(
                token = jwtToken.value ?: "",
                requests = listOf(ApiCallRequest("main", "login-status", buildJsonObject {})),
                chunkSize = 1
            )
            val latency = System.currentTimeMillis() - start
            Pair(response.results.isNotEmpty(), latency)
        } catch (_: Exception) {
            val latency = System.currentTimeMillis() - start
            Pair(false, latency)
        }
    }

    fun decodeJwtPayload(jwt: String?): String? {
        if (jwt == null) return null
        return try {
            val parts = jwt.split(".")
            if (parts.size >= 2) {
                val decoded = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING)
                String(decoded, Charsets.UTF_8)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun logout() = viewModelScope.launch {
        sessionManager.clearSession()
    }
}