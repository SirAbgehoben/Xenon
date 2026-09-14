package org.abgehoben.xenon.data.remote.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val jwt: String? = null,
    val requireTwoFactorEmailCode: Boolean = false,
    val requireTOTP: Boolean = false,
    val multipleAccounts: List<Account>? = null,
    val userId: Int? = null,
    val user: User? = null
)