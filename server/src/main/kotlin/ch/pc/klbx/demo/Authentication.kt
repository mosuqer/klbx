package ch.pc.klbx.demo

import ch.pc.klbx.ApplicationRoles
import ch.pc.klbx.ErrorCode
import ch.pc.klbx.auth.LoginData
import ch.pc.klbx.auth.LoginResult
import ch.pc.klbx.encode
import ch.pc.klbx.shared.TokenPair
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
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaInstant


// TODO: Add Claims and copy them from the refdresh token to the new granted token
// TODO: Unit Test the Login and refresh mechanism


private val jwtAuth = "jwtAuth"
private val LOGGER = KtorSimpleLogger("ch.pc.klbx.demo.Authentication")

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
            if (user.username != user.password) throw HttpStatusException.Forbidden(
                "Invalid username or password",
                ErrorCode.LoginDenied
            )

            call.respond(HttpStatusCode.OK, jwt.createTokenResponse(user.username))
        }

        post("/refresh") {
            val body = call.receive<TokenPair>()
            call.respond(jwt.createNewToken(body))
        }
    }
    return ApplicationAuth(myRealm)
}

private class JWTTokens(
    val audience: String,
    val issuer: String,
    secret: String
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
    ): LoginResult {
        return LoginResult(
            token = createJwtToken(username),
            refreshToken = createJwtToken(username, isRefreshToken = true),
            success = true
        )
    }

    fun createNewToken(token: TokenPair): LoginResult {
        // THE REFRESH TOKEN MUST BE VALID!!
        refreshTokenVerifier.verify(token.refreshToken)


        val jwt = JWT.decode(token.token)
        val tokenValidAndExpired = try {
            tokenVerifier.verify(jwt)
            null
        } catch (_: TokenExpiredException) {
            jwt
        }

        if (tokenValidAndExpired == null) throw HttpStatusException.Forbidden(
            "Invalid Token to refresh",
            ErrorCode.InvalidToken
        )
        val roles = tokenValidAndExpired.getClaim("roles").asList(String::class.java)

        val newToken = JWT.create()
            .withSubject(tokenValidAndExpired.subject)
            .withIssuer(tokenValidAndExpired.issuer)
            .withClaim("roles", roles)
            .withAudience(audience)
            .withExpiresAt(fromNow(10.minutes).toJavaInstant())
            .sign(algorithm)
        return LoginResult(newToken, createJwtToken(tokenValidAndExpired.subject, isRefreshToken = true), true)
    }

    private fun createJwtToken(username: String, isRefreshToken: Boolean = false, expireIn: Duration? = null): String {
        val roles = when (username) {
            "admin" -> listOf(ApplicationRoles.ADMIN)
            else -> listOf(ApplicationRoles.USER)
        }.encode()

        val refreshToken = JWT.create()
            .withAudience(audience + if (isRefreshToken) "refresh" else "")
            .withIssuer(issuer)
            .withSubject(username)
            .withClaim("roles", roles)
            .withExpiresAt(fromNow(expireIn ?: if (isRefreshToken) 30.minutes else 1.seconds).toJavaInstant())
            .sign(algorithm)
        return refreshToken
    }

}


