package ch.pc.klbx.demo

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.response.respond

class HttpStatusException(override val message: String,val statusCode: HttpStatusCode) : Exception(message) {

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