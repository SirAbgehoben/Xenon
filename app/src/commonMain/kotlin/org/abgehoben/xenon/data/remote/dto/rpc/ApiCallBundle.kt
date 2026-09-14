package org.abgehoben.xenon.data.remote.dto.rpc

import kotlinx.serialization.Serializable

@Serializable
data class ApiCallBundle(
    val bundleVersion: String = "deadbeef00", //TODO placeholder
    val requests: List<ApiCallRequest>
)