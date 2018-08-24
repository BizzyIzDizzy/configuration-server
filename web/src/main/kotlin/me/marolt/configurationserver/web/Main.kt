package me.marolt.configurationserver.web

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import me.marolt.configurationserver.api.IConfigurationLoader
import me.marolt.configurationserver.utils.DEVELOPMENT_MODE
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.pf4j.CompoundPluginDescriptorFinder
import org.pf4j.DefaultPluginManager
import org.pf4j.ManifestPluginDescriptorFinder
import org.pf4j.PluginDescriptorFinder
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.concurrent.thread

val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    if (DEVELOPMENT_MODE) {
        Configurator.setLevel("io.netty", Level.DEBUG)
        Configurator.setLevel("org.eclipse.jetty", Level.DEBUG)
    }

    logger.info { "Initializing plugin system!" }
    val path = FileSystems.getDefault().getPath(File("plugins/bin").absolutePath)
    val pluginManager = object : DefaultPluginManager(path) {
        override fun createPluginDescriptorFinder(): PluginDescriptorFinder {
            return CompoundPluginDescriptorFinder()
                    .add(ManifestPluginDescriptorFinder())
        }
    }
    pluginManager.loadPlugins()
    pluginManager.startPlugins()

    val plugins: List<IConfigurationLoader> = pluginManager.getExtensions(IConfigurationLoader::class.java)

    for (plugin in plugins) {
        println (plugin.configurableOptions)
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