package org.abgehoben.xenon.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.abgehoben.xenon.MainViewModel
import org.abgehoben.xenon.ui.screens.calendar.CalendarScreen
import org.abgehoben.xenon.ui.screens.settings.SettingsScreen
import org.abgehoben.xenon.ui.screens.timetable.TimetableScreen

@Composable
fun XenonNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Timetable.route,
        modifier = modifier
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