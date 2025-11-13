package ch.pc.klbx.client

import ch.pc.klbx.ErrorCode
import ch.pc.klbx.auth.LoginData
import ch.pc.klbx.auth.LoginResult
import ch.pc.klbx.shared.TokenPair
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val HttpStatusCode.isSuccess: Boolean
    get() {
        return this.value in 200..299
    }


class HttpException(override val message: String, val statusCode: HttpStatusCode, val errorCode: ErrorCode? = null) :
    Exception(message)

data class HttpResponse<T>(
    val status: HttpStatusCode,
    val body: T?,
    val errorCode: ErrorCode? = null,
    val errorMessage: String? = null,
    private val ktorRequest: HttpResponse
) {
    companion object {
        suspend inline fun <reified ResponseType> fromResponse(response: HttpResponse): ch.pc.klbx.client.HttpResponse<ResponseType> {
            val status = response.status
            val body = if (status.isSuccess) response.body<ResponseType>() else null
            val errorCode = if (!status.isSuccess) ErrorCode.UnknownError else null
            val errorMessage = response.body<String>()
            return HttpResponse(status, body, errorCode, errorMessage, response)
        }
    }

    fun <R> onSuccess(handler: (T) -> R): R? {
        if (status == HttpStatusCode.OK && body != null)
            return handler(body)
        else return null
    }

    fun <R> withBody(handler: (T) -> R): R? {
        if (status == HttpStatusCode.OK && body != null) {
            return handler(body)
        } else {
            throw HttpException(message = "Status: $status, Body: $body", statusCode = status, errorCode = errorCode)
        }
    }

}

/**
 * Encaplusates access to the Klbx Backend.
 */
class KlbxClient(
    val host: String,
    val port: Int
) {
    private var token: String? = null
    private var refreshToken: String? = null
    private val baseUrl = lazy { "http://$host:$port" }

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
        install(Auth) {
            bearer {
                loadTokens {
                    if (token == null || refreshToken == null)
                        null
                    else BearerTokens(token!!, refreshToken!!)
                }
                refreshTokens {
                    if (token == null || refreshToken == null)
                        return@refreshTokens null
                    // we only have access to the provied KtorClient, helper functions from this
                    // class ccannot be used.
                    val refreshResult = client.post(baseUrl.value + "/refresh") {
                        headers.set(HttpHeaders.ContentType, "application/json")
                        setBody(TokenPair(token!!, refreshToken!!))
                    }
                    refreshResult.body<LoginResult>().let {
                        if (it.success) {
                            BearerTokens(it.token, it.refreshToken)
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }

    suspend fun login(username: String, password: String) {
        val loginResult: LoginResult = post("login", LoginData(username, password))
        if (loginResult.success) {
            this.token = loginResult.token
            this.refreshToken = loginResult.refreshToken
        } else {
            this.token = null
            this.refreshToken = null
        }
    }

    suspend fun echo(body: String): ch.pc.klbx.client.HttpResponse<String> {
        return ch.pc.klbx.client.HttpResponse.fromResponse(post("echo", body))
    }

    private suspend inline fun <reified RequestType, reified ResponseType> post(
        path: String,
        body: RequestType
    ): ResponseType {
        return request<RequestType>(path, body).body<ResponseType>()
    }

    private suspend inline fun <reified RequestType> request(
        path: String,
        body: RequestType,
        config: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse = httpClient.request(baseUrl.value + if (path.startsWith('/')) path else "/$path") {
        method = HttpMethod.Post
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
        setBody(body)
        config()
    }
}