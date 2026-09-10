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
        startDestination = NavigationItem.Timetable.route,
        modifier = modifier
    ) {
        composable(NavigationItem.Timetable.route) {
            TimetableScreen(viewModel)
        }
        composable(NavigationItem.Calendar.route) {
            CalendarScreen(viewModel)
        }
        composable(NavigationItem.Settings.route) {
            SettingsScreen(viewModel)
        }
    }
}