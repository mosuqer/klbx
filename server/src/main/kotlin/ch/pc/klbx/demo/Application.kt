package ch.pc.klbx.demo

import com.joelromanpr.commandline.ktx.Parser
import com.joelromanpr.commandline.ktx.annotations.Option
import com.joelromanpr.commandline.ktx.core.ParserResult
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.Json
import kotlin.system.exitProcess

@com.joelromanpr.commandline.ktx.annotations.Application(
    name = "KLbx server",
    description = "Runs a KLbx server, provides all functionality of the loanboox platform",
)
data class ProgramOptions(
    @Option(
        longName = "env",
        helpText = "the environment this instance runs in. dev|prod",
        default = "dev",
        required = false
    ) var env: String? = null,

    @Option(
        longName = "host",
        shortName = 'h',
        helpText = "the interface to bind to. Defaults to all interfaces",
        default = "0.0.0.0",
    )
    val host: String = "0.0.0.0",
    @Option(
        longName = "port",
        shortName = 'p',
        helpText = "the port to bind to. Defaults to 8080",
        default = "8079",
    )
    val port: Int = 8079,
)


fun main(args: Array<String>) {

    val params = parseParams(args)
    val env = applicationEnvironment {
        this.log = KtorSimpleLogger("io.ktor.server.Application")
        configure(
            when (params.env) {
                "dev" -> "application-local.conf"
                "prod" -> "application.conf"
                else -> throw Exception("invalid environment: ${params.env} must be dev or prod")
            }
        )
    }
    val config = serverConfig(env) {
        this.developmentMode = params.env != "dev"
        this.watchPaths = listOf(SystemFileSystem.resolve(Path(".")).toString())
        module(Application::module)
    }


    embeddedServer(Netty, config) {
        connector {
            host = params.host
            port = params.port
        }
    }.start(wait = true)
}


private fun parseParams(args: Array<String>): ProgramOptions {
    val parser = Parser()
    return when (val result = parser.parseArguments<ProgramOptions>(args)) {
        is ParserResult.Parsed -> {
            result.value
        }

        is ParserResult.NotParsed -> {
            // Errors are collected for you
            result.errors.forEach { println("Error: ${it.message}") }
            println("\n" + parser.generateHelpText<ProgramOptions>())
            exitProcess(1)
        }
    }
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }
    val auth = installAuthentication()

    routing {

        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
        auth.secured(this) {
            post("/echo") {
                val text = call.receiveText()
                call.respondText(text)
            }
        }
    }
}