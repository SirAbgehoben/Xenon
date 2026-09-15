package org.abgehoben.xenon.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.staticCompositionLocalOf

val LocalDynamicColorScheme = staticCompositionLocalOf<(darkTheme: Boolean, dynamicColor: Boolean, defaultDark: ColorScheme, defaultLight: ColorScheme) -> ColorScheme> {
    { darkTheme, _, defaultDark, defaultLight ->
        if (darkTheme) defaultDark else defaultLight
    }
}