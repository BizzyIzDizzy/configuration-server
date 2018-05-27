package me.marolt.configurationserver

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import me.marolt.configurationserver.utils.IControl
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

class ServerControl : IControl {
    private val logger = KotlinLogging.logger {}

    private var server: NettyApplicationEngine? = null
        @Synchronized get
        @Synchronized set

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

    override suspend fun start() {
        logger.info { "Starting server!" }

        val serverInstance = embeddedServer(Netty, port = 8080) {
            routing {
                get("/") {
                    call.respondText("HelloWorld", ContentType.Text.Plain)
                }
            }
        }

        serverInstance.start(wait = false)

        server = serverInstance

        logger.info { "Server started!" }
    }
}