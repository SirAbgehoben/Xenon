package org.abgehoben.xenon

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.abgehoben.xenon.di.appModule
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.KoinTest
import org.koin.test.verify.verify

class KoinModuleTest : KoinTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun verifyKoinConfiguration() {
        appModule.verify(
            extraTypes = listOf(
                Application::class,
                Context::class,
                DataStore::class,
                Preferences::class
            )
        )
    }
}