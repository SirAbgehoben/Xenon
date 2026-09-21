package org.abgehoben.xenon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import org.abgehoben.xenon.platform.LocalDynamicColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun XenonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val dynamicResolver = LocalDynamicColorScheme.current
    val colorScheme = dynamicResolver(
        darkTheme,
        dynamicColor,
        DarkColorScheme,
        LightColorScheme
    )

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}