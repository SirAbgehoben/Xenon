package org.abgehoben.xenon.data.remote.dto.rpc

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiCallRequest(
    val moduleName: String,
    val endpointName: String,
    val parameters: JsonElement
)