package org.abgehoben.xenon.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import org.abgehoben.xenon.R

sealed class NavigationItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    data object Timetable : NavigationItem("timetable", R.string.nav_timetable, Icons.AutoMirrored.Filled.List)
    data object Calendar : NavigationItem("calendar", R.string.nav_calendar, Icons.Default.CalendarMonth)
    data object Settings : NavigationItem("settings", R.string.nav_settings, Icons.Default.Settings)

    companion object {
        val bottomNavItems = listOf(Timetable, Calendar, Settings)
    }
}