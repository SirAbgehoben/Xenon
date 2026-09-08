package org.abgehoben.xenon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.abgehoben.xenon.data.local.ThemeMode
import org.abgehoben.xenon.ui.screens.LoginScreen
import org.abgehoben.xenon.ui.screens.calendar.CalendarScreen
import org.abgehoben.xenon.ui.screens.settings.SettingsScreen
import org.abgehoben.xenon.ui.screens.timetable.TimetableScreen
import org.abgehoben.xenon.ui.theme.XenonTheme

sealed class Screen(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    object Timetable : Screen("timetable", R.string.nav_timetable, Icons.AutoMirrored.Filled.List)
    object Calendar : Screen("calendar", R.string.nav_calendar, Icons.Default.CalendarMonth)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val appState by viewModel.appState.collectAsState()
            val userSettings by viewModel.userSettings.collectAsState()

            // Calculate active theme mode dynamically from DataStore settings
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
                        is AppState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                LoadingIndicator()
                            }
                        }
                        is AppState.LoginRequired -> {
                            LoginScreen(
                                error = null,
                                onLogin = viewModel::login
                            )
                        }
                        is AppState.Error -> {
                            LoginScreen(
                                error = state.message,
                                onLogin = viewModel::login
                            )
                        }
                        is AppState.Authenticated -> {
                            MainAppContent(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val items = listOf(Screen.Timetable, Screen.Calendar, Screen.Settings)

    Scaffold(
        // Do not let outer scaffold double-pad the top insets
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(stringResource(screen.labelRes)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Timetable.route,
            // Only pad for the bottom navigation bar
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Screen.Timetable.route) {
                TimetableScreen(viewModel)
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel)
            }
        }
    }
}