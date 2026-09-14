package org.abgehoben.xenon.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import xenon.app.generated.resources.Res
import xenon.app.generated.resources.*

sealed class NavigationItem(
    val route: String,
    val labelRes: StringResource,
    val icon: ImageVector
) {
    data object Timetable : NavigationItem("timetable", Res.string.nav_timetable, Icons.AutoMirrored.Filled.List)
    data object Calendar : NavigationItem("calendar", Res.string.nav_calendar, Icons.Default.CalendarMonth)
    data object Settings : NavigationItem("settings", Res.string.nav_settings, Icons.Default.Settings)

    companion object {
        val bottomNavItems = listOf(Timetable, Calendar, Settings)
    }

}