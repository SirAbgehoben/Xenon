package org.abgehoben.xenon.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import org.abgehoben.xenon.MainViewModel
import org.abgehoben.xenon.data.local.model.ThemeMode
import org.abgehoben.xenon.ui.components.LoadingView
import org.abgehoben.xenon.ui.screens.auth.LoginScreen
import org.abgehoben.xenon.ui.state.AppState
import org.abgehoben.xenon.ui.theme.Dimens
import org.abgehoben.xenon.ui.theme.XenonTheme

@Composable
fun XenonApp(viewModel: MainViewModel) {
    val appState by viewModel.appState.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    val isDark = when (userSettings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    XenonTheme(
        darkTheme = isDark,
        dynamicColor = userSettings.dynamicColor
    ) {
        AnimatedContent(
            targetState = appState,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "AppStateTransition"
        ) { state ->
            when (state) {
                is AppState.Loading -> LoadingView()
                is AppState.LoginRequired -> LoginScreen(
                    error = null,
                    onLogin = viewModel::login
                )
                is AppState.Error -> LoginScreen(
                    error = state.message,
                    onLogin = viewModel::login
                )
                is AppState.Authenticated -> AuthenticatedMainContent(viewModel)
            }
        }
    }
}

@Composable
private fun AuthenticatedMainContent(viewModel: MainViewModel) {
    val navController = rememberNavController()

    Scaffold(
        contentWindowInsets = WindowInsets(
            Dimens.SpacingNone,
            Dimens.SpacingNone,
            Dimens.SpacingNone,
            Dimens.SpacingNone
        ),
        bottomBar = { XenonBottomBar(navController = navController) }
    ) { innerPadding ->
        XenonNavHost(
            navController = navController,
            viewModel = viewModel,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        )
    }
}