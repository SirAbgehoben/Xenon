package org.abgehoben.xenon.data.remote.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val userId: Int,
    val firstname: String,
    val lastname: String,
    val institutionName: String
)