package me.marolt.configurationserver

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import kotlin.concurrent.thread

val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
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