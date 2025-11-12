package ch.pc.klbx.demo

import ch.pc.klbx.auth.LoginData
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.TokenExpiredException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaInstant


// TODO: Add Claims and copy them from the refdresh token to the new granted token
// TODO: Unit Test the Login and refresh mechanism


private val jwtAuth = "jwtAuth"

@Serializable
data class Token(
    val token: String,
    val refreshToken: String
)

data class ApplicationAuth(private val realm: String) {

    fun secured(route: Route, optional: Boolean = false, body: Route.() -> Unit) {
        route.authenticate(jwtAuth, optional = optional) {
            body(this)
        }
    }
}


fun Application.installAuthentication(): ApplicationAuth {
    val secret = environment.config.propertyOrNull("jwt.secret")?.getString()
        ?: throw Exception("config not found: available keys: " + environment.config.keys().toList())
    val issuer = environment.config.property("jwt.issuer").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val myRealm = environment.config.property("jwt.realm").getString()
    val jwt = JWTTokens(audience, issuer, secret)

    install(Authentication) {
        jwt(jwtAuth) {
            this.realm = myRealm
            validate { credential ->
                JWTPrincipal(credential.payload)
            }
            verifier(
                JWT
                    .require(Algorithm.HMAC256(secret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
            )
        }
    }

    routing {
        post("/login") {
            val user = call.receive<LoginData>()
            // Check username and password
            if (user.username != user.password) throw Exception("Invalid username or password")

            call.respond(HttpStatusCode.OK, jwt.createTokenResponse(user.username))
        }

        post("/refresh") {
            val body = call.receive<Token>()
            call.respond(jwt.createNewToken(body))
        }
    }
    return ApplicationAuth(myRealm)
}

private class JWTTokens(
    val audience: String,
    val issuer: String,
    val secret: String
) {

    private val algorithm = Algorithm.HMAC256(secret)
    private val tokenVerifier = JWT.require(Algorithm.HMAC256(secret))
        .withAudience(audience)
        .withIssuer(issuer)
        .ignoreIssuedAt()
        .build()

    private val refreshTokenVerifier = JWT.require(Algorithm.HMAC256(secret))
        .withAudience(audience + "refresh")
        .withIssuer(issuer)
        .build()

    fun createTokenResponse(
        username: String,
    ): Token {
        val roles = when(username) {
            "admin" -> mapOf("ROLE_ADMIN" to true)
            else -> mapOf("ROLE_USER" to true)
        }
        val token = JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withExpiresAt(fromNow(1.seconds).toJavaInstant())
            .withSubject(username)
            .withPayload(roles)
            .sign(algorithm)
        val refreshToken = createRefreshToken(username)
        val toenResponse = Token(token, refreshToken)
        return toenResponse
    }

    private fun createRefreshToken(username: String): String {
        val roles = when(username) {
            "admin" -> mapOf("ROLE_ADMIN" to true)
            else -> mapOf("ROLE_USER" to true)
        }

        val refreshToken = JWT.create()
            .withAudience(audience + "refresh")
            .withIssuer(issuer)
            .withSubject(username)
            .withPayload(roles)
            .withExpiresAt(fromNow(30.minutes).toJavaInstant())
            .sign(algorithm)
        return refreshToken
    }

    fun createNewToken(token: Token): Token {
        val refreshToken = refreshTokenVerifier.verify(token.refreshToken)
        val tokenValidAndExpired = try {
            tokenVerifier.verify(token.token)
            null
        } catch (_: TokenExpiredException) {
            refreshToken
        }

        if (tokenValidAndExpired == null) throw Exception("Invalid Token to refresh")
        val newPayload = tokenValidAndExpired.claims.map { it.key to it.value }.toMap()
        val newToken = JWT.create()
            .withSubject(tokenValidAndExpired.subject)
            .withIssuer(tokenValidAndExpired.issuer)
            .withAudience(*tokenValidAndExpired.audience.toTypedArray())
            .withExpiresAt(fromNow(10.minutes).toJavaInstant())
            .sign(algorithm)
        return Token(newToken, createRefreshToken(tokenValidAndExpired.subject))
    }

}


