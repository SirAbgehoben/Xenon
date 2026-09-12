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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.abgehoben.xenon.data.local.SessionManager
import org.abgehoben.xenon.data.local.SettingsManager
import org.abgehoben.xenon.data.local.model.UserSettings
import org.abgehoben.xenon.data.remote.SchulmanagerApi
import org.abgehoben.xenon.ui.state.AppState
import org.abgehoben.xenon.util.ErrorFormatter

class MainViewModel(
    private val sessionManager: SessionManager,
    settingsManager: SettingsManager,
    private val api: SchulmanagerApi,
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
        Log.e(TAG, "Uncaught session error: ${throwable.message}", throwable)
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
                    ensureStudentData(token)
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
            val parentStudent =
                (userObj?.get("associatedParents") as? JsonArray)?.firstNotNullOfOrNull {
                    (it as? JsonObject)?.get("student") as? JsonObject
                }
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