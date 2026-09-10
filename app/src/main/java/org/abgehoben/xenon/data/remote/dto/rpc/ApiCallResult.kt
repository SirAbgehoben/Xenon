package org.abgehoben.xenon.data.remote.dto.rpc

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiCallResult(
    val status: Int,
    val data: JsonElement? = null
)