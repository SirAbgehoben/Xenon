package org.abgehoben.xenon

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import org.abgehoben.xenon.platform.LocalDynamicColorScheme
import org.abgehoben.xenon.ui.navigation.XenonApp
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val dynamicResolver: (Boolean, Boolean, ColorScheme, ColorScheme) -> ColorScheme = { darkTheme, dynamicColor, defaultDark, defaultLight ->
                when {
                    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    }
                    darkTheme -> defaultDark
                    else -> defaultLight
                }
            }

            CompositionLocalProvider(LocalDynamicColorScheme provides dynamicResolver) {
                val viewModel: MainViewModel = koinViewModel()
                XenonApp(viewModel = viewModel)
            }
        }
    }
}