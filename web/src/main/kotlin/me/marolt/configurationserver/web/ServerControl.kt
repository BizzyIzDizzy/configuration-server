package me.marolt.configurationserver.web

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import me.marolt.configurationserver.utils.DEVELOPMENT_MODE
import me.marolt.configurationserver.utils.IControl
import me.marolt.configurationserver.utils.fullMessage
import me.marolt.configurationserver.utils.tryGetEnvironmentVariable
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

class ServerControl : IControl {
    private val logger = KotlinLogging.logger {}

    private var server: NettyApplicationEngine? = null
        @Synchronized get
        @Synchronized set

    override suspend fun start() {
        logger.info { "Starting server!" }

        val port = tryGetEnvironmentVariable("CFG_SERVER_PORT")?.toInt() ?: 8080

        val serverInstance =
                if (DEVELOPMENT_MODE)
                    embeddedServer(Netty, port = port, module = Application::mainModule, watchPaths = listOf("configuration-server"))
                else
                    embeddedServer(Netty, port = port, module = Application::mainModule)

        serverInstance.start(wait = false)

        server = serverInstance

        logger.info { "Server started! Listening at port $port." }
    }

    override suspend fun stop() {
        logger.info { "Stopping server!" }

        val serverInstance = server

        if (serverInstance == null) {
            logger.warn { "Server isn't started!" }
        } else {
            serverInstance.stop(1000, 1000, TimeUnit.MILLISECONDS)
            logger.info { "Server stopped!" }
        }
    }
}

fun Application.mainModule() {
    install(CORS) {
        method(HttpMethod.Options)
        anyHost()
    }

    install(StatusPages) {
        exceptionHandling()
    }

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            serializeNulls()
            generateNonExecutableJson()
        }
    }

    routing {
        root()
    }
}

fun Routing.root() {
    val logger = KotlinLogging.logger {}

    route("{path...}") {
        method(HttpMethod.Get) {
            handle {
                logger.info { "Called ${this.context.request.toLogString()}" }
                call.respondText { "test" }
            }
        }
    }

    post("/refreshAll") {
        logger.info { "Refreshing all configurations on all paths." }
        call.respondText { "DONE" }
    }

    post("/refresh") {
        val request = call.receive<RefreshRequest>()
        logger.info { "Refreshing configurations on paths: [${request.paths.joinToString()}]" }
        call.respondText { "DONE" }
    }

    get("/favicon.ico") {
        logger.warn { "No favicon." }
        call.respond(HttpStatusCode.NotFound, "No favicon")
    }
}

fun StatusPages.Configuration.exceptionHandling() {
    val logger = KotlinLogging.logger {}

    statusFile(HttpStatusCode.NotFound, HttpStatusCode.Unauthorized, filePattern = "error#.html")

    exception<Exception> { cause ->
        logger.error(cause) { "Unexpected exception occurred!" }
        if (DEVELOPMENT_MODE) {
            call.respond(HttpStatusCode.InternalServerError, "Unexpected exception occurred! Additional details: ${cause.fullMessage()}")
        } else {
            call.respond(HttpStatusCode.InternalServerError, "Unexpected exception occurred!")
        }
    }
}