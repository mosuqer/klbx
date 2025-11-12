package ch.pc.klbx.demo

import ch.pc.klbx.auth.LoginData
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
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
import java.util.Date


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
            verifier(JWT
                .require(Algorithm.HMAC256(secret))
                .withAudience(audience)
                .withIssuer(issuer)
                .build())
        }
    }

    routing {
        post("/login") {
            val user = call.receive<LoginData>()
            // Check username and password
            if (user.username != user.password) throw Exception("Invalid username or password")

            call.respond(HttpStatusCode.OK, jwt.createTokenResponse(user))
        }

        post("/refresh") {
            val body = call.receive<Token>()
            val token = JWT.decode(body.token)
            val refreshToken = JWT.decode(body.refreshToken)
        }
    }
    return ApplicationAuth(myRealm)
}

private class JWTTokens(
    val audience: String,
    val issuer: String,
    val secret: String
) {
    val verifier = JWT.require(Algorithm.HMAC256(secret))
        .withAudience(audience)
        .withIssuer(issuer)
        .build()
    fun createTokenResponse(
        user: LoginData,
    ): Token {
        val algorithm = Algorithm.HMAC256(secret)
        val token = JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("username", user.username)
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(algorithm)
        val refreshToken = JWT.create()
            .withAudience(audience + "refresh")
            .withIssuer(issuer)
            .withClaim("username", user.username)
            .withExpiresAt(Date(System.currentTimeMillis() + (60000 * 10)))
            .sign(algorithm)
        val toenResponse = Token(token, refreshToken)
        return toenResponse
    }

    fun createNewToken(token: Token) {

    }

}


