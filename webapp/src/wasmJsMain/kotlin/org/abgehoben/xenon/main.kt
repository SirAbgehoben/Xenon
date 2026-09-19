package org.abgehoben.xenon

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.abgehoben.xenon.di.appModule
import org.abgehoben.xenon.ui.navigation.XenonApp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(appModule)
    }

    ComposeViewport(document.body!!) {
        val viewModel: MainViewModel = koinViewModel()
        XenonApp(viewModel = viewModel)
    }
}