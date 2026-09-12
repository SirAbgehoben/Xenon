package org.abgehoben.xenon

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.abgehoben.xenon.data.local.SessionManager
import org.abgehoben.xenon.data.local.SettingsManager
import org.abgehoben.xenon.data.local.model.UserSettings
import org.abgehoben.xenon.data.remote.SchulmanagerApi
import org.abgehoben.xenon.data.repository.util.StudentResolver
import org.abgehoben.xenon.ui.state.AppState
import org.abgehoben.xenon.util.ErrorFormatter

class MainViewModel(
    private val sessionManager: SessionManager,
    private val studentResolver: StudentResolver,
    private val api: SchulmanagerApi,
    settingsManager: SettingsManager,
    application: Application
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "MainViewModel"
    }

    val userSettings: StateFlow<UserSettings> = settingsManager.userSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    private val _appState = MutableStateFlow<AppState>(AppState.Loading)
    val appState: StateFlow<AppState> = _appState

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Session error: ${throwable.message}", throwable)
        _appState.value = AppState.Error(ErrorFormatter.format(getApplication(), throwable))
    }

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch(coroutineExceptionHandler) {
            sessionManager.jwtToken.collectLatest { token ->
                if (token != null) {
                    _appState.value = AppState.Authenticated(token)
                    studentResolver.resolveAndSync(token)
                } else {
                    _appState.value = AppState.LoginRequired
                }
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch(coroutineExceptionHandler) {
            _appState.value = AppState.Loading
            try {
                val response = api.login(username, password)
                if (response.jwt != null) {
                    sessionManager.saveJwtToken(response.jwt)
                    studentResolver.resolveAndSync(response.jwt)
                } else {
                    _appState.value = AppState.Error(
                        getApplication<Application>().getString(R.string.error_login_failed)
                    )
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _appState.value = AppState.Error(ErrorFormatter.format(getApplication(), e))
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch(coroutineExceptionHandler) {
            sessionManager.clearSession()
            _appState.value = AppState.LoginRequired
        }
    }
}