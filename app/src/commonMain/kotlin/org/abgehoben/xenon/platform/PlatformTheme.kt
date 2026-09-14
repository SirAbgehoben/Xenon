package org.abgehoben.xenon.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
expect fun platformColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    defaultDark: ColorScheme,
    defaultLight: ColorScheme
): ColorScheme