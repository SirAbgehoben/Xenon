package org.abgehoben.xenon.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okio.Path.Companion.toPath
import org.abgehoben.xenon.platform.AndroidPlatformInfo
import org.abgehoben.xenon.platform.AndroidPlatformNotifier
import org.abgehoben.xenon.platform.PlatformInfo
import org.abgehoben.xenon.platform.PlatformNotifier
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val androidPlatformModule = module {
    single<HttpClientEngine> {
        OkHttp.create {
            config {
                val dispatcher = okhttp3.Dispatcher().apply {
                    maxRequests = 64
                    maxRequestsPerHost = 16
                }
                dispatcher(dispatcher)
                connectTimeout(10, TimeUnit.SECONDS)
                readTimeout(15, TimeUnit.SECONDS)
                writeTimeout(15, TimeUnit.SECONDS)
                callTimeout(15, TimeUnit.SECONDS)
                retryOnConnectionFailure(true)
            }
        }
    }

    single<DataStore<Preferences>>(named("sessionDataStore")) {
        val context = get<Context>()
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { context.filesDir.resolve("session.preferences_pb").absolutePath.toPath() }
        )
    }

    single<DataStore<Preferences>>(named("settingsDataStore")) {
        val context = get<Context>()
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { context.filesDir.resolve("settings.preferences_pb").absolutePath.toPath() }
        )
    }

    single<PlatformInfo> { AndroidPlatformInfo() }
    single<PlatformNotifier> { AndroidPlatformNotifier(context = get()) }
}