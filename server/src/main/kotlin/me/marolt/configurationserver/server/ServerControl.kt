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
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import me.marolt.configurationserver.core.ConfigurationProcessingPipeline
import me.marolt.configurationserver.core.PipelineConfiguration
import me.marolt.configurationserver.core.PluginRepository
import me.marolt.configurationserver.utils.DEVELOPMENT_MODE
import me.marolt.configurationserver.utils.IControl
import me.marolt.configurationserver.utils.fullMessage
import me.marolt.configurationserver.utils.tryGetEnvironmentVariable
import mu.KLogging
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
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

    val pipelines = ConcurrentHashMap<String, ConfigurationProcessingPipeline>()

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
        trace {
            logger.trace { it.buildText() }
        }

        route("/api") {
            route("/pipelines") {
                route("/{pipelineName}") {
                    get("/") {
                        val pipelineName = call.parameters["pipelineName"]

                        val pipeline = pipelines[pipelineName]
                        if (pipeline != null) {
                            val configurations = pipeline.run()
                            call.respond(configurations)
                        } else {
                            call.respondText("Pipeline with name '$pipelineName' does not exists!",
                                ContentType.Text.Plain, HttpStatusCode.InternalServerError)
                        }
                    }
                }
            }

            route("/manage") {

                route("/plugins") {
                    get("/refresh") {
                        pluginRepository.reload()
                        call.respondText("Done")
                    }
                }

                route("/pipelines") {
                    get("/") {
                        // return all pipeline
                        call.respond(pipelines.values.map { p -> p.config })
                    }

                    post("/") {
                        // create a new pipeline
                        val newPipelineConfiguration = call.receive<PipelineConfiguration>()

                        if (pipelines[newPipelineConfiguration.name] != null) {
                            call.respondText("Pipeline with name '${newPipelineConfiguration.name}' already exists!",
                                ContentType.Text.Plain, HttpStatusCode.InternalServerError)
                        } else {
                            val newPipeline = ConfigurationProcessingPipeline.configurePipeline(newPipelineConfiguration, pluginRepository)
                            pipelines[newPipelineConfiguration.name] = newPipeline
                            call.respondText("Done")
                        }
                    }

                    route("/{pipelineName}") {
                        get("/") {
                            // return a specific pipeline
                            val pipelineName = call.parameters["pipelineName"]
                            val pipeline = pipelines[pipelineName]

                            if (pipeline != null) {
                                call.respond(pipeline.config)
                            } else {
                                call.respondText("Pipeline does not exists!", ContentType.Text.Plain, HttpStatusCode.NotFound)
                            }
                        }

                        put("/") {
                            val pipelineName = call.parameters["pipelineName"]
                            val pipeline = pipelines[pipelineName]

                            if (pipeline != null) {
                                val newPipelineConfiguration = call.receive<PipelineConfiguration>()

                                if (newPipelineConfiguration.name != pipelineName) {
                                    call.respondText("Incoming and existing pipeline names should match! Incoming: '${newPipelineConfiguration.name}', existing: '$pipelineName'.",
                                        ContentType.Text.Plain, HttpStatusCode.InternalServerError)
                                } else {
                                    val newPipeline = ConfigurationProcessingPipeline.configurePipeline(newPipelineConfiguration, pluginRepository)
                                    pipelines[pipelineName] = newPipeline
                                    call.respondText("Done")
                                }
                            } else {
                                call.respondText("Pipeline does not exists!", ContentType.Text.Plain, HttpStatusCode.NotFound)
                            }

                            

                        }

                        delete("/") {
                            val pipelineName = call.parameters["pipelineName"]
                            val pipeline = pipelines[pipelineName]

                            if (pipeline != null) {
                                pipelines.remove(pipelineName)
                                call.respondText("Done")
                            } else {
                                call.respondText("Pipeline does not exists!", ContentType.Text.Plain, HttpStatusCode.NotFound)
                            }
                        }
                    }
                }
            }
        }

        get("/favicon.ico") {
            logger.warn { "No favicon." }
            call.respond(HttpStatusCode.NotFound, "No favicon")
        }
    }
}