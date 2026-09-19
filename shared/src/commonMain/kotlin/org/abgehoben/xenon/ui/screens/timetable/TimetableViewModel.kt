package org.abgehoben.xenon.ui.screens.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.abgehoben.xenon.data.local.SessionManager
import org.abgehoben.xenon.data.local.SettingsManager
import org.abgehoben.xenon.data.local.model.UserSettings
import org.abgehoben.xenon.data.model.timetable.TimetableGrid
import org.abgehoben.xenon.data.model.timetable.TimetableViewMode
import org.abgehoben.xenon.data.repository.TimetableRepository
import org.abgehoben.xenon.platform.AppLogger
import org.abgehoben.xenon.util.ErrorFormatter
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.milliseconds
import org.abgehoben.xenon.util.*

class TimetableViewModel(
    private val timetableRepository: TimetableRepository,
    private val sessionManager: SessionManager,
    private val settingsManager: SettingsManager
) : ViewModel() {
    companion object {
        private const val TAG = "TimetableViewModel"
        private const val TIMEOUT_MS = 15_000L
    }

    val userSettings: StateFlow<UserSettings> = settingsManager.userSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    private val _timetableGrid = MutableStateFlow<TimetableGrid?>(null)
    val timetableGrid: StateFlow<TimetableGrid?> = _timetableGrid

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset

    private val _timetableViewMode = MutableStateFlow(TimetableViewMode.WEEKLY)
    val timetableViewMode: StateFlow<TimetableViewMode> = _timetableViewMode

    private var currentJob: Job? = null

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        AppLogger.e(TAG, "Timetable error: ${throwable.message}", throwable)
        _syncError.value = ErrorFormatter.format(throwable)
        _isSyncing.value = false
        _isRefreshing.value = false
    }

    init {
        viewModelScope.launch(coroutineExceptionHandler) {
            val settings = settingsManager.userSettings.firstOrNull() ?: UserSettings()
            _timetableViewMode.value = settings.defaultViewMode

            val today = LocalDate.now().dayOfWeek
            if (settings.weekendAdvance && (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY)) {
                _weekOffset.value = 1
            }
            loadCurrentTimetable()
        }
    }

    fun nextWeek() = navigateWeek(1)
    fun prevWeek() = navigateWeek(-1)

    private fun navigateWeek(delta: Int) {
        _weekOffset.value += delta
        loadCurrentTimetable()
    }

    fun setTimetableViewMode(mode: TimetableViewMode) {
        _timetableViewMode.value = mode
    }

    fun refreshData(forceRefresh: Boolean = true) {
        _isRefreshing.value = true
        loadCurrentTimetable(forceRefresh = forceRefresh)
    }

    private fun loadCurrentTimetable(forceRefresh: Boolean = false) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch(coroutineExceptionHandler) {
            val token = sessionManager.jwtToken.firstOrNull() ?: return@launch
            _isSyncing.value = true
            _syncError.value = null

            try {
                withTimeout(TIMEOUT_MS.milliseconds) {
                    val baseMonday = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
                    val targetMonday = baseMonday.plusWeeks(_weekOffset.value.toLong())
                    val grid = timetableRepository.getFullTimetable(token, targetMonday, forceRefresh)
                    _timetableGrid.value = grid
                }

                if (userSettings.value.preloadWeeks) {
                    launch {
                        try {
                            val baseMonday = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
                            timetableRepository.getFullTimetable(token, baseMonday.plusWeeks(1), false)
                            timetableRepository.getFullTimetable(token, baseMonday.minusWeeks(1), false)
                        } catch (_: Throwable) {}
                    }
                }
            } catch (e: Throwable) {
                if (e !is CancellationException) {
                    _syncError.value = ErrorFormatter.format(e)
                }
            } finally {
                _isSyncing.value = false
                _isRefreshing.value = false
            }
        }
    }
}