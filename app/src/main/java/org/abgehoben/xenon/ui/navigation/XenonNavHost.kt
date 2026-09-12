package org.abgehoben.xenon.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.abgehoben.xenon.ui.screens.calendar.CalendarScreen
import org.abgehoben.xenon.ui.screens.calendar.CalendarViewModel
import org.abgehoben.xenon.ui.screens.settings.SettingsScreen
import org.abgehoben.xenon.ui.screens.settings.SettingsViewModel
import org.abgehoben.xenon.ui.screens.timetable.TimetableScreen
import org.abgehoben.xenon.ui.screens.timetable.TimetableViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun XenonNavHost(
    navController: NavHostController,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavigationItem.Timetable.route,
        modifier = modifier
    ) {
        composable(NavigationItem.Timetable.route) {
            val timetableViewModel: TimetableViewModel = koinViewModel()
            TimetableScreen(viewModel = timetableViewModel)
        }
        composable(NavigationItem.Calendar.route) {
            val calendarViewModel: CalendarViewModel = koinViewModel()
            CalendarScreen(viewModel = calendarViewModel)
        }
        composable(NavigationItem.Settings.route) {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onLogout = onLogout
            )
        }
    }
}