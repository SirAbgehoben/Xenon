package org.abgehoben.xenon.platform

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

actual fun createHttpClientEngine(): HttpClientEngine = OkHttp.create {
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