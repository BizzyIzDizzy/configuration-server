package me.marolt.configurationserver.web

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import me.marolt.configurationserver.utils.DEVELOPMENT_MODE
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import kotlin.concurrent.thread

val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    if (DEVELOPMENT_MODE) {
        Configurator.setLevel("io.netty", Level.DEBUG)
        Configurator.setLevel("org.eclipse.jetty", Level.DEBUG)
    }

    val serverControl = ServerControl()

    logger.info { "Starting server control!" }
    val job = async {
        serverControl.start()
    }

    runBlocking {
        job.await()
    }

    // add shutdown hook to allow graceful exit
    Runtime.getRuntime().addShutdownHook(thread(start = false) {
        logger.info { "Received interrupt signal - shutting down!" }
        runBlocking {
            serverControl.stop()
        }
    })
}