package ch.pc.klbx.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginData(
    val username: String,
    val password: String,
)
@Serializable
data class LoginResult(
    val token: String,
    val refreshToken: String,
    val success: Boolean,
)

