package org.abgehoben.xenon.data.remote

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.abgehoben.xenon.data.local.SessionManager
import org.abgehoben.xenon.data.remote.dto.auth.LoginResponse
import org.abgehoben.xenon.data.remote.dto.rpc.ApiCallBundle
import org.abgehoben.xenon.data.remote.dto.rpc.ApiCallRequest
import org.abgehoben.xenon.data.remote.dto.rpc.ApiCallResponse
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class SchulmanagerApi(private val sessionManager: SessionManager) {
    companion object {
        private const val TAG = "SchulmanagerApi"
    }

    private val baseUrl = "https://login.schulmanager-online.de"
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val client = HttpClient(OkHttp) {
        engine {
            config {
                val dispatcher = okhttp3.Dispatcher()
                dispatcher.maxRequests = 64
                dispatcher.maxRequestsPerHost = 16
                dispatcher(dispatcher)

                connectTimeout(10, TimeUnit.SECONDS)
                readTimeout(15, TimeUnit.SECONDS)
                writeTimeout(15, TimeUnit.SECONDS)
                callTimeout(15, TimeUnit.SECONDS)
                retryOnConnectionFailure(true)
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 20000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 20000
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d(TAG, message)
                }
            }
            level = LogLevel.INFO
        }
        defaultRequest {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Accept, "application/json, text/plain, */*")
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            header("Origin", baseUrl)
            header("Referer", "$baseUrl/")
        }
    }

    suspend fun login(
        username: String,
        password: String,
        twoFactorCode: String? = null,
        userId: Int? = null
    ): LoginResponse {
        return safeNetworkCall {
            Log.d(TAG, "Starting login request for user: $username")
            val payload = buildJsonObject {
                put("emailOrUsername", username)
                put("password", password)
                put("hash", JsonNull)
                put("mobileApp", false)
                put("twoFactorCode", twoFactorCode)
                put("userId", userId)
            }

            val response: HttpResponse = client.post("$baseUrl/api/login") {
                setBody(payload)
            }

            val status = response.status
            Log.d(TAG, "Login response status: $status")

            if (status != HttpStatusCode.OK) {
                val errBody = response.bodyAsText()
                Log.e(TAG, "Login failed: $errBody")
                throw Exception("Login failed ($status): $errBody")
            }

            response.body()
        }
    }

    /**
     * Executes requests in chunks of [chunkSize] in parallel.
     */
    suspend fun fetchCallsChunked(
        token: String,
        requests: List<ApiCallRequest>,
        chunkSize: Int = 2
    ): ApiCallResponse = coroutineScope {
        val chunks = requests.chunked(chunkSize)
        Log.d(TAG, "Executing ${requests.size} requests in ${chunks.size} chunks PARALLELLY")

        val deferreds = chunks.mapIndexed { index, chunk ->
            async {
                Log.d(TAG, "Posting chunk ${index + 1}/${chunks.size} starting...")
                val payload = ApiCallBundle(requests = chunk)
                val response: HttpResponse = client.post("$baseUrl/api/calls") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header("X-Skip-Bearer-Token-Renewal", "true")
                    setBody(payload)
                }

                if (response.status == HttpStatusCode.Unauthorized) {
                    Log.w(TAG, "Received 401 Unauthorized, clearing session")
                    sessionManager.clearSession()
                    throw IOException("Session expired (401)")
                }

                if (response.status != HttpStatusCode.OK) {
                    val errBody = response.bodyAsText()
                    Log.e(TAG, "Chunk ${index + 1} failed with status ${response.status}: $errBody")
                    throw IOException("API call chunk ${index + 1} failed with status ${response.status}: $errBody")
                }

                val body: ApiCallResponse = response.body()

                if (body.results.any { it.status == 401 }) {
                    sessionManager.clearSession()
                    throw IOException("Session expired (401)")
                }
                if (body.results.any { it.status == 429 }) {
                    throw IOException("Rate limit (429)")
                }

                Log.d(TAG, "Chunk ${index + 1}/${chunks.size} finished successfully")
                body.results
            }
        }

        val allResults = deferreds.flatMap { it.await() }
        ApiCallResponse(results = allResults)
    }

    private suspend fun <T> safeNetworkCall(block: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: UnknownHostException) {
                throw IOException("UnknownHostException", e)
            } catch (e: ConnectException) {
                throw IOException("ConnectException", e)
            } catch (e: SocketTimeoutException) {
                throw IOException("SocketTimeoutException", e)
            } catch (e: Exception) {
                throw e
            }
        }
    }
}