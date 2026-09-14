package org.abgehoben.xenon

import android.app.Application
import org.abgehoben.xenon.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class XenonApplication : Application() {
    companion object {
        var instance: XenonApplication? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        startKoin {
            androidContext(this@XenonApplication)
            modules(appModules)
        }
    }
}