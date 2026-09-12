package org.abgehoben.xenon.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.abgehoben.xenon.ui.screens.calendar.CalendarRoute
import org.abgehoben.xenon.ui.screens.settings.SettingsRoute
import org.abgehoben.xenon.ui.screens.timetable.TimetableRoute

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
            TimetableRoute()
        }
        composable(NavigationItem.Calendar.route) {
            CalendarRoute()
        }
        composable(NavigationItem.Settings.route) {
            SettingsRoute(onLogout = onLogout)
        }
    }
}