package org.abgehoben.xenon

import android.app.Application
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import org.abgehoben.xenon.data.local.SessionManager
import org.abgehoben.xenon.data.local.SettingsManager
import org.abgehoben.xenon.data.local.model.ThemeMode
import org.abgehoben.xenon.data.local.model.UserSettings
import org.abgehoben.xenon.data.model.calendar.ProcessedEvent
import org.abgehoben.xenon.data.model.system.CacheStats
import org.abgehoben.xenon.data.model.timetable.TimetableGrid
import org.abgehoben.xenon.data.remote.SchulmanagerApi
import org.abgehoben.xenon.data.remote.dto.rpc.ApiCallRequest
import org.abgehoben.xenon.data.repository.CalendarRepository
import org.abgehoben.xenon.data.repository.TimetableRepository
import org.abgehoben.xenon.ui.state.AppState
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "MainViewModel"
        private const val TIMEOUT_NAV_MS = 15_000L
        private const val TIMEOUT_SYNC_MS = 20_000L
    }

    private val sessionManager = SessionManager(application)
    private val settingsManager = SettingsManager(application)
    private val api = SchulmanagerApi(sessionManager)
    val calendarRepository = CalendarRepository(api)
    val timetableRepository = TimetableRepository(api, sessionManager, calendarRepository)

    val userSettings: StateFlow<UserSettings> = settingsManager.userSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    private val _appState = MutableStateFlow<AppState>(AppState.Loading)
    val appState: StateFlow<AppState> = _appState

    private val _timetableGrid = MutableStateFlow<TimetableGrid?>(null)
    val timetableGrid: StateFlow<TimetableGrid?> = _timetableGrid

    private val _calendarEvents = MutableStateFlow<Map<LocalDate, List<ProcessedEvent>>>(emptyMap())
    val calendarEvents: StateFlow<Map<LocalDate, List<ProcessedEvent>>> = _calendarEvents

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset

    private val _isWeeklyView = MutableStateFlow(true)
    val isWeeklyView: StateFlow<Boolean> = _isWeeklyView

    private val _lastScheduleLoadDurationMs = MutableStateFlow<Long?>(null)
    val lastScheduleLoadDurationMs: StateFlow<Long?> = _lastScheduleLoadDurationMs

    private var currentSyncJob: Job? = null
    private var currentNavJob: Job? = null

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught coroutine exception: ${throwable.message}", throwable)
        handleSyncError(throwable)
    }

    init {
        viewModelScope.launch(coroutineExceptionHandler) {
            val settings = settingsManager.userSettings.firstOrNull() ?: UserSettings()
            _isWeeklyView.value = settings.defaultViewWeekly

            val today = LocalDate.now().dayOfWeek
            if (settings.weekendAdvance && (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY)) {
                _weekOffset.value = 1
            }
        }
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch(coroutineExceptionHandler) {
            sessionManager.jwtToken.collectLatest { token ->
                if (token != null) {
                    _appState.value = AppState.Authenticated(token)
                    ensureStudentData(token)
                    startTieredSync(token)
                } else {
                    _appState.value = AppState.LoginRequired
                }
            }
        }
    }

    private suspend fun ensureStudentData(token: String) {
        try {
            val statusObj = api.getLoginStatus(token)
            val userObj = statusObj?.get("user") as? JsonObject

            val directStudent = userObj?.get("associatedStudent") as? JsonObject
            val parentStudent = (userObj?.get("associatedParents") as? JsonArray)
                ?.mapNotNull { (it as? JsonObject)?.get("student") as? JsonObject }
                ?.firstOrNull()
            val pluralStudent = (userObj?.get("associatedStudents") as? JsonArray)
                ?.firstOrNull() as? JsonObject

            val resolved = directStudent ?: parentStudent ?: pluralStudent ?: userObj

            if (resolved != null) {
                sessionManager.saveStudentData(resolved.toString())
                Log.d(TAG, "Saved resolved student data: $resolved")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve student info from login-status", e)
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch(coroutineExceptionHandler) {
            _appState.value = AppState.Loading
            try {
                val response = api.login(username, password)
                if (response.jwt != null) {
                    sessionManager.saveJwtToken(response.jwt)
                    ensureStudentData(response.jwt)
                } else {
                    _appState.value = AppState.Error(
                        getApplication<Application>().getString(R.string.error_login_failed)
                    )
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _appState.value = AppState.Error(formatErrorMessage(e))
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch(coroutineExceptionHandler) {
            sessionManager.clearSession()
            _timetableGrid.value = null
            _calendarEvents.value = emptyMap()
            _lastScheduleLoadDurationMs.value = null
            _appState.value = AppState.LoginRequired
        }
    }

    fun refreshData(forceRefresh: Boolean = true) {
        viewModelScope.launch(coroutineExceptionHandler) {
            val token = (appState.value as? AppState.Authenticated)?.token
                ?: sessionManager.jwtToken.firstOrNull()

            if (token != null) {
                _appState.value = AppState.Authenticated(token)
                _isRefreshing.value = true
                _syncError.value = null
                startTieredSync(token, forceRefresh = forceRefresh)
            } else {
                _appState.value = AppState.LoginRequired
            }
        }
    }

    fun nextWeek() = navigateWeek(1)
    fun prevWeek() = navigateWeek(-1)
    fun currentWeek() {
        if (_weekOffset.value != 0) {
            _weekOffset.value = 0
            refreshCurrentState(forceRefresh = false)
        }
    }

    private fun navigateWeek(delta: Int) {
        _weekOffset.value += delta
        refreshCurrentState(forceRefresh = false)
    }

    private fun refreshCurrentState(forceRefresh: Boolean) {
        val state = _appState.value
        if (state is AppState.Authenticated) {
            currentNavJob?.cancel()
            currentNavJob = viewModelScope.launch(coroutineExceptionHandler) {
                _isSyncing.value = true
                _syncError.value = null
                val startTime = System.currentTimeMillis()
                try {
                    withTimeout(TIMEOUT_NAV_MS.milliseconds) {
                        val baseMonday = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
                        val targetMonday = baseMonday.plusWeeks(_weekOffset.value.toLong())
                        val grid = timetableRepository.getFullTimetable(state.token, targetMonday, forceRefresh)
                        _timetableGrid.value = grid
                        _lastScheduleLoadDurationMs.value = System.currentTimeMillis() - startTime
                    }
                } catch (e: Throwable) {
                    if (e !is CancellationException) {
                        handleSyncError(e)
                    }
                } finally {
                    _isSyncing.value = false
                }
            }
        }
    }

    fun toggleViewMode() {
        _isWeeklyView.value = !_isWeeklyView.value
    }

    fun startTieredSync(token: String, forceRefresh: Boolean = false) {
        currentSyncJob?.cancel()
        currentNavJob?.cancel()
        currentSyncJob = viewModelScope.launch(coroutineExceptionHandler) {
            _isSyncing.value = true
            _syncError.value = null
            val startTime = System.currentTimeMillis()
            try {
                withTimeout(TIMEOUT_SYNC_MS.milliseconds) {
                    val baseMonday = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
                    val currentTargetMonday = baseMonday.plusWeeks(_weekOffset.value.toLong())

                    supervisorScope {
                        val calendarDeferred = async {
                            runCatching { calendarRepository.getCalendarEvents(token, forceRefresh) }.getOrNull()
                        }

                        val grid = timetableRepository.getFullTimetable(token, currentTargetMonday, forceRefresh)
                        val calendarEvents = calendarDeferred.await()

                        _timetableGrid.value = grid
                        _lastScheduleLoadDurationMs.value = System.currentTimeMillis() - startTime

                        if (calendarEvents != null) {
                            _calendarEvents.value = calendarEvents
                        }
                    }

                    _appState.value = AppState.Authenticated(token)
                }

                if (userSettings.value.preloadWeeks) {
                    launch {
                        try {
                            val baseMonday = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
                            timetableRepository.getFullTimetable(token, baseMonday.plusWeeks(1), false)
                            timetableRepository.getFullTimetable(token, baseMonday.minusWeeks(1), false)
                        } catch (_: Throwable) {
                            // Non-fatal preload failure
                        }
                    }
                }
            } catch (e: Throwable) {
                if (e !is CancellationException) {
                    handleSyncError(e)
                }
            } finally {
                _isSyncing.value = false
                _isRefreshing.value = false
            }
        }
    }

    private fun handleSyncError(e: Throwable) {
        Log.e(TAG, "Sync failed: ${e.message}", e)
        val msg = e.message ?: ""
        if (msg.contains("401") || msg.contains("Session expired")) {
            viewModelScope.launch(coroutineExceptionHandler) {
                sessionManager.clearSession()
                _appState.value = AppState.LoginRequired
            }
        } else {
            _syncError.value = formatErrorMessage(e)
        }
    }

    private fun formatErrorMessage(e: Throwable): String {
        val app = getApplication<Application>()
        val msg = e.message ?: ""

        return when {
            msg.contains("401") || msg.contains("Session expired") -> app.getString(R.string.error_session_expired)
            msg.contains("429") || msg.contains("Rate limit") -> app.getString(R.string.error_rate_limited)
            e is UnknownHostException || e.cause is UnknownHostException -> app.getString(R.string.error_no_internet)
            e is ConnectException || e.cause is ConnectException -> app.getString(R.string.error_server_unreachable)
            e is SocketTimeoutException || e.cause is SocketTimeoutException -> app.getString(R.string.error_timeout)
            !e.localizedMessage.isNullOrEmpty() -> e.localizedMessage!!
            else -> app.getString(R.string.error_network_generic)
        }
    }

    // Settings proxies
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsManager.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsManager.setDynamicColor(enabled) }
    fun setDefaultViewWeekly(enabled: Boolean) = viewModelScope.launch {
        settingsManager.setDefaultViewWeekly(enabled)
        _isWeeklyView.value = enabled
    }
    fun setMergeLessons(enabled: Boolean) = viewModelScope.launch { settingsManager.setMergeLessons(enabled) }
    fun setWeekendAdvance(enabled: Boolean) = viewModelScope.launch {
        settingsManager.setWeekendAdvance(enabled)
        val today = LocalDate.now().dayOfWeek
        if (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY) {
            _weekOffset.value = if (enabled) 1 else 0
            refreshCurrentState(forceRefresh = false)
        }
    }
    fun setScaleBreaks(enabled: Boolean) = viewModelScope.launch { settingsManager.setScaleBreaks(enabled) }
    fun setPreloadWeeks(enabled: Boolean) = viewModelScope.launch { settingsManager.setPreloadWeeks(enabled) }

    fun clearAppCache() {
        timetableRepository.clearAllCache()
        calendarRepository.clearCache()
        _timetableGrid.value = null
        _calendarEvents.value = emptyMap()
        _lastScheduleLoadDurationMs.value = null
        refreshData(forceRefresh = true)
    }

    suspend fun fetchIcalUrl(renew: Boolean = false): String? {
        val token = (appState.value as? AppState.Authenticated)?.token ?: return null
        return calendarRepository.getIcalUrl(token, renew)
    }

    fun getTimetableCacheStats(): CacheStats = timetableRepository.getCacheStats()

    suspend fun pingServer(): Pair<Boolean, Long> {
        val start = System.currentTimeMillis()
        return try {
            val response = api.fetchCallsChunked(
                token = (appState.value as? AppState.Authenticated)?.token ?: "",
                requests = listOf(ApiCallRequest("main", "login-status", buildJsonObject {})),
                chunkSize = 1
            )
            val latency = System.currentTimeMillis() - start
            Pair(response.results.isNotEmpty(), latency)
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            null
        }
    }
}