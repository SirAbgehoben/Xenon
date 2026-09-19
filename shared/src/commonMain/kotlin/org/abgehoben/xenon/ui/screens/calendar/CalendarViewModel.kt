package org.abgehoben.xenon.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.abgehoben.xenon.data.local.SessionManager
import org.abgehoben.xenon.data.model.calendar.ProcessedEvent
import org.abgehoben.xenon.data.repository.CalendarRepository
import org.abgehoben.xenon.platform.AppLogger
import org.abgehoben.xenon.util.ErrorFormatter
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.milliseconds

class CalendarViewModel(
    private val calendarRepository: CalendarRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    companion object {
        private const val TAG = "CalendarViewModel"
        private const val TIMEOUT_MS = 20_000L
    }

    private val _calendarEvents = MutableStateFlow<Map<LocalDate, List<ProcessedEvent>>>(emptyMap())
    val calendarEvents: StateFlow<Map<LocalDate, List<ProcessedEvent>>> = _calendarEvents

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    private var currentJob: Job? = null

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        AppLogger.e(TAG, "Calendar error: ${throwable.message}", throwable)
        _syncError.value = ErrorFormatter.format(throwable)
        _isSyncing.value = false
        _isRefreshing.value = false
    }

    init {
        loadEvents(forceRefresh = false)
    }

    fun refreshData(forceRefresh: Boolean = true) {
        _isRefreshing.value = true
        loadEvents(forceRefresh = forceRefresh)
    }

    private fun loadEvents(forceRefresh: Boolean) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch(coroutineExceptionHandler) {
            val token = sessionManager.jwtToken.firstOrNull() ?: return@launch
            _isSyncing.value = true
            _syncError.value = null

            try {
                withTimeout(TIMEOUT_MS.milliseconds) {
                    val events = calendarRepository.getCalendarEvents(token, forceRefresh)
                    _calendarEvents.value = events
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