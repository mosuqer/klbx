package ch.pc.klbx.shared

import kotlinx.serialization.Serializable

@Serializable
data class TokenPair(
    val token: String,
    val refreshToken: String
)
