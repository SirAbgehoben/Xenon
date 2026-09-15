package org.abgehoben.xenon.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.abgehoben.xenon.data.remote.SchulmanagerApi
import org.abgehoben.xenon.platform.AppLogger
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val networkModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
            prettyPrint = false
        }
    }

    single {
        val json = get<Json>()
        val engine = get<HttpClientEngine>()
        val baseUrl = "https://login.schulmanager-online.de"

        HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 20_000
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        AppLogger.d("KtorClient", message)
                    }
                }
                level = LogLevel.INFO
            }
            defaultRequest {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Accept, "application/json, text/plain, */*")
                header("User-Agent", "Mozilla/5.0 Xenon Multiplatform Client")
                header("Origin", baseUrl)
                header("Referer", "$baseUrl/")
            }
        }
    }

    singleOf(::SchulmanagerApi)
}