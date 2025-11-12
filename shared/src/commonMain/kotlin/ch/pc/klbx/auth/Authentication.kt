package ch.pc.klbx.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginData(
    val username: String,
    val password: String,
)

