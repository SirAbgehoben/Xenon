package org.abgehoben.xenon

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.abgehoben.xenon.data.*
import java.time.DayOfWeek
import java.time.LocalDate

sealed class AppState {
    object Loading : AppState()
    object LoginRequired : AppState()
    data class Authenticated(val token: String) : AppState()
    data class Error(val message: String) : AppState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "MainViewModel"
    }

    private val sessionManager = SessionManager(application)
    private val api = SchulmanagerApi(sessionManager)
    private val repository = TimetableRepository(api)

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

    private var currentSyncJob: Job? = null
    private var currentNavJob: Job? = null

    init {
        val today = LocalDate.now().dayOfWeek
        if (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY) {
            _weekOffset.value = 1
        }
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            sessionManager.jwtToken.collectLatest { token ->
                if (token != null) {
                    _appState.value = AppState.Authenticated(token)
                    startTieredSync(token)
                } else {
                    _appState.value = AppState.LoginRequired
                }
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _appState.value = AppState.Loading
            try {
                val response = api.login(username, password)
                if (response.jwt != null) {
                    sessionManager.saveJwtToken(response.jwt)
                } else {
                    _appState.value = AppState.Error("Login failed")
                }
            } catch (e: Exception) {
                _appState.value = AppState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _timetableGrid.value = null
            _calendarEvents.value = emptyMap()
            _appState.value = AppState.LoginRequired
        }
    }

    fun refreshData(forceRefresh: Boolean = true) {
        val state = _appState.value
        if (state is AppState.Authenticated) {
            if (forceRefresh) {
                _isRefreshing.value = true
                startTieredSync(state.token, forceRefresh = true)
            } else {
                refreshCurrentState(false)
            }
        }
    }

    fun nextWeek() = navigateWeek(1)
    fun prevWeek() = navigateWeek(-1)
    fun currentWeek() {
        if (_weekOffset.value != 0) {
            _weekOffset.value = 0
            refreshCurrentState(false)
        }
    }

    private fun navigateWeek(delta: Int) {
        _weekOffset.value += delta
        refreshCurrentState(false)
    }

    private fun refreshCurrentState(forceRefresh: Boolean) {
        val state = _appState.value
        if (state is AppState.Authenticated) {
            // FIX: Cancel any in-flight navigation request so it cannot overwrite the active week later
            currentNavJob?.cancel()
            currentNavJob = viewModelScope.launch {
                _isSyncing.value = true
                _syncError.value = null
                try {
                    val baseMonday = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
                    val targetMonday = baseMonday.plusWeeks(_weekOffset.value.toLong())
                    val grid = repository.getFullTimetable(state.token, targetMonday, forceRefresh)
                    _timetableGrid.value = grid
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        handleSyncError(e)
                    }
                } finally {
                    _isSyncing.value = false
                }
            }
        }
    }

    fun toggleViewMode() { _isWeeklyView.value = !_isWeeklyView.value }

    fun startTieredSync(token: String, forceRefresh: Boolean = false) {
        currentSyncJob?.cancel()
        currentNavJob?.cancel()
        currentSyncJob = viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            try {
                val baseMonday = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
                val currentTargetMonday = baseMonday.plusWeeks(_weekOffset.value.toLong())

                val calendarDeferred = async { repository.getCalendarEvents(token) }
                val currentGrid = repository.getFullTimetable(token, currentTargetMonday, forceRefresh)

                _timetableGrid.value = currentGrid
                _calendarEvents.value = calendarDeferred.await()

                _appState.value = AppState.Authenticated(token)
                _isSyncing.value = false

                // Preload immediately adjacent weeks gently without flooding the API with 9 requests
                launch {
                    try {
                        repository.getFullTimetable(token, baseMonday.plusWeeks(1), false)
                        repository.getFullTimetable(token, baseMonday.minusWeeks(1), false)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    handleSyncError(e)
                }
            } finally {
                _isSyncing.value = false
                _isRefreshing.value = false
            }
        }
    }

    private fun handleSyncError(e: Exception) {
        Log.e(TAG, "Sync failed: ${e.message}")
        if (e.message?.contains("401") == true || e.message?.contains("Session expired") == true) {
            viewModelScope.launch {
                sessionManager.clearSession()
                _appState.value = AppState.LoginRequired
            }
        } else {
            _syncError.value = e.message
        }
    }
}