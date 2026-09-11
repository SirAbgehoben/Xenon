package org.abgehoben.xenon.ui.state

sealed interface AppState {
    data object Loading : AppState
    data object LoginRequired : AppState
    data class Authenticated(val token: String) : AppState
    data class Error(val message: String) : AppState
}