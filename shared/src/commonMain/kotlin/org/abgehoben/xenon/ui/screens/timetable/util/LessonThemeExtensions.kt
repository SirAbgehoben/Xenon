package org.abgehoben.xenon.ui.screens.timetable.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.abgehoben.xenon.data.model.timetable.LessonStatus
import org.abgehoben.xenon.ui.theme.StatusSubstitution

data class LessonColors(
    val accent: Color,
    val container: Color,
    val onContainer: Color
)

@Composable
fun LessonStatus.colors(): LessonColors {
    return when (this) {
        LessonStatus.FREE_PERIOD -> LessonColors(
            accent = Color.Transparent,
            container = MaterialTheme.colorScheme.surfaceContainerLowest,
            onContainer = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LessonStatus.HOLIDAY -> LessonColors(
            accent = MaterialTheme.colorScheme.tertiary,
            container = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
            onContainer = MaterialTheme.colorScheme.onTertiaryContainer
        )
        LessonStatus.SUBSTITUTION -> LessonColors(
            accent = StatusSubstitution,
            container = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            onContainer = MaterialTheme.colorScheme.onSecondaryContainer
        )
        LessonStatus.CANCELLED -> LessonColors(
            accent = MaterialTheme.colorScheme.error,
            container = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            onContainer = MaterialTheme.colorScheme.onErrorContainer
        )
        LessonStatus.REGULAR -> LessonColors(
            accent = MaterialTheme.colorScheme.primary,
            container = MaterialTheme.colorScheme.surfaceContainerHigh,
            onContainer = MaterialTheme.colorScheme.onSurface
        )
    }
}