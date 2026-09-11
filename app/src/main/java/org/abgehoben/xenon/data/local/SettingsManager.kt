package org.abgehoben.xenon.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.abgehoben.xenon.data.local.datastore.dataStore
import org.abgehoben.xenon.data.local.model.ThemeMode
import org.abgehoben.xenon.data.model.timetable.TimetableViewMode
import org.abgehoben.xenon.data.local.model.UserSettings

class SettingsManager(private val context: Context) {
    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_DEFAULT_VIEW_MODE = stringPreferencesKey("default_view_mode")
        private val KEY_MERGE_LESSONS = booleanPreferencesKey("merge_lessons")
        private val KEY_WEEKEND_ADVANCE = booleanPreferencesKey("weekend_advance")
        private val KEY_SCALE_BREAKS = booleanPreferencesKey("scale_breaks")
        private val KEY_PRELOAD_WEEKS = booleanPreferencesKey("preload_weeks")
    }

    val userSettings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val themeModeStr = prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        val viewModeStr = prefs[KEY_DEFAULT_VIEW_MODE] ?: TimetableViewMode.WEEKLY.name

        UserSettings(
            themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: true,
            defaultViewMode = runCatching { TimetableViewMode.valueOf(viewModeStr) }.getOrDefault(TimetableViewMode.WEEKLY),
            mergeLessons = prefs[KEY_MERGE_LESSONS] ?: true,
            weekendAdvance = prefs[KEY_WEEKEND_ADVANCE] ?: true,
            scaleBreaks = prefs[KEY_SCALE_BREAKS] ?: true,
            preloadWeeks = prefs[KEY_PRELOAD_WEEKS] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    suspend fun setDefaultViewMode(mode: TimetableViewMode) = context.dataStore.edit { it[KEY_DEFAULT_VIEW_MODE] = mode.name }
    suspend fun setMergeLessons(enabled: Boolean) = context.dataStore.edit { it[KEY_MERGE_LESSONS] = enabled }
    suspend fun setWeekendAdvance(enabled: Boolean) = context.dataStore.edit { it[KEY_WEEKEND_ADVANCE] = enabled }
    suspend fun setScaleBreaks(enabled: Boolean) = context.dataStore.edit { it[KEY_SCALE_BREAKS] = enabled }
    suspend fun setPreloadWeeks(enabled: Boolean) = context.dataStore.edit { it[KEY_PRELOAD_WEEKS] = enabled }
}