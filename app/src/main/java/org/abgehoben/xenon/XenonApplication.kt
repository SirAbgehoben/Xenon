package org.abgehoben.xenon

import android.app.Application
import org.abgehoben.xenon.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class XenonApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            // Android logger (Level.DEBUG in debug, Level.ERROR in release)
            androidLogger(Level.DEBUG)
            androidContext(this@XenonApplication)
            modules(appModules)
        }
    }
}