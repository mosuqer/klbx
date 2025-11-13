package ch.pc.klbx.demo

import ch.pc.klbx.ErrorCode
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.response.respond

class HttpStatusException(override val message: String, val statusCode: HttpStatusCode, val errorCode: ErrorCode? = null) :
    Exception(message) {
    companion object {
        fun BadRequest(message: String, errorCode: ErrorCode? = null) = HttpStatusException(
            message,
            HttpStatusCode.BadRequest,
            errorCode
        )
        fun Forbidden(message: String, errorCode: ErrorCode? = null) = HttpStatusException(
            message, HttpStatusCode.Forbidden, errorCode
        )
    }
}


fun Application.installErrorHandling() {
    intercept(ApplicationCallPipeline.Call) {
        try {
            this.proceed()
        } catch (exception: HttpStatusException) {
            call.respond( exception.statusCode, exception.message)
        }
    }
}