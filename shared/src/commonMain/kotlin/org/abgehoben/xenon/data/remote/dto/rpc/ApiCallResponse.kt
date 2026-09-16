package org.abgehoben.xenon.data.remote.dto.rpc

import kotlinx.serialization.Serializable

@Serializable
data class ApiCallResponse(
    val results: List<ApiCallResult>
)