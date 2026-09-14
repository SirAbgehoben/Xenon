package org.abgehoben.xenon.data.remote.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int? = null,
    val firstname: String? = null,
    val lastname: String? = null
)