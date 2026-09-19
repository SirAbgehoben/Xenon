package org.abgehoben.xenon.data.remote

import org.abgehoben.xenon.platform.AppLogger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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

class SchulmanagerApi(
    private val sessionManager: SessionManager,
    private val client: HttpClient
) {
    companion object {
        private const val TAG = "SchulmanagerApi"
    }

    private val baseUrl = "https://login.schulmanager-online.de"

    suspend fun login(
        username: String,
        password: String,
        twoFactorCode: String? = null,
        userId: Int? = null
    ): LoginResponse {
        return safeNetworkCall {
            AppLogger.d(TAG, "Starting login request for user: $username")
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
            AppLogger.d(TAG, "Login response status: $status")

            if (status != HttpStatusCode.OK) {
                val errBody = response.bodyAsText()
                AppLogger.e(TAG, "Login failed: $errBody")
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
        AppLogger.d(TAG, "Executing ${requests.size} requests in ${chunks.size} chunks PARALLELLY")

        val deferreds = chunks.mapIndexed { index, chunk ->
            async {
                AppLogger.d(TAG, "Posting chunk ${index + 1}/${chunks.size} starting...")
                val payload = ApiCallBundle(requests = chunk)
                val response: HttpResponse = client.post("$baseUrl/api/calls") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header("X-Skip-Bearer-Token-Renewal", "true")
                    setBody(payload)
                }

                if (response.status == HttpStatusCode.Unauthorized) {
                    AppLogger.w(TAG, "Received 401 Unauthorized, clearing session")
                    sessionManager.clearSession()
                    throw Exception("Session expired (401)")
                }

                if (response.status != HttpStatusCode.OK) {
                    val errBody = response.bodyAsText()
                    AppLogger.e(TAG, "Chunk ${index + 1} failed with status ${response.status}: $errBody")
                    throw Exception("API call chunk ${index + 1} failed with status ${response.status}: $errBody")
                }

                val body: ApiCallResponse = response.body()

                if (body.results.any { it.status == 401 }) {
                    sessionManager.clearSession()
                    throw Exception("Session expired (401)")
                }
                if (body.results.any { it.status == 429 }) {
                    throw Exception("Rate limit (429)")
                }

                AppLogger.d(TAG, "Chunk ${index + 1}/${chunks.size} finished successfully")
                body.results
            }
        }

        val allResults = deferreds.flatMap { it.await() }
        ApiCallResponse(results = allResults)
    }

    suspend fun getLoginStatus(token: String): JsonObject? {
        return safeNetworkCall {
            val response: HttpResponse = client.post("$baseUrl/api/login-status") {
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(buildJsonObject {})
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<JsonObject>()
            } else {
                null
            }
        }
    }

    private suspend fun <T> safeNetworkCall(block: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            block()
        }
    }
}