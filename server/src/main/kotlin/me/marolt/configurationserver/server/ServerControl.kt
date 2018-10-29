//       DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
//                   Version 2, December 2004
//
// Copyright (C) 2004 Sam Hocevar <sam@hocevar.net>
//
// Everyone is permitted to copy and distribute verbatim or modified
// copies of this license document, and changing it is allowed as long
// as the name is changed.
//
//            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
//   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
//
//  0. You just DO WHAT THE FUCK YOU WANT TO.

package me.marolt.configurationserver.server

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import me.marolt.configurationserver.api.IPlugin
import me.marolt.configurationserver.core.PluginRepository
import me.marolt.configurationserver.utils.DEVELOPMENT_MODE
import me.marolt.configurationserver.utils.IControl
import me.marolt.configurationserver.utils.fullMessage
import me.marolt.configurationserver.utils.tryGetEnvironmentVariable
import mu.KLogging
import mu.KotlinLogging
import java.util.concurrent.TimeUnit.MILLISECONDS

class ServerControl : IControl {
    companion object : KLogging()

    private lateinit var server: NettyApplicationEngine

    override suspend fun start() {
        logger.info { "Starting server!" }

        val port = tryGetEnvironmentVariable("CFG_SERVER_PORT")?.toInt() ?: 8080

        val serverInstance = if (DEVELOPMENT_MODE)
            embeddedServer(Netty, port = port, module = Application::main, watchPaths = listOf("server", "api", "core"))
        else
            embeddedServer(Netty, port = port, module = Application::main)

        serverInstance.start(wait = false)
        server = serverInstance

        logger.info { "Server started! Listening on port $port." }
    }

    override suspend fun stop() {
        logger.info { "Stopping server!" }

        val serverInstance = server

        serverInstance.stop(1000, 1000, MILLISECONDS)

        logger.info { "Server stopped!" }
    }
}

fun Application.main() {
    val logger = KotlinLogging.logger {}

    val pluginRoot = tryGetEnvironmentVariable("CFG_SERVER_PLUGIN_ROOT") ?: "./plugins/bin"
    val pluginRepository = PluginRepository(pluginRoot, "me.marolt.configurationserver.plugins")

    install(CORS) {
        method(HttpMethod.Options)
        anyHost()
    }

    install(StatusPages) {
        exception<Exception> { cause ->
            logger.error(cause) { "Unexpected exception occurred!" }
            if (DEVELOPMENT_MODE) {
                call.respond(HttpStatusCode.InternalServerError, "Unexpected exception occurred! Additional details: ${cause.fullMessage()}")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Unexpected exception occurred!")
            }
        }
    }

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            serializeNulls()
        }
    }

    routing {
        get("/") {
            call.respondText(pluginRepository.loadPlugins<IPlugin>().joinToString { p -> p.id.toString() })
        }
    }
}