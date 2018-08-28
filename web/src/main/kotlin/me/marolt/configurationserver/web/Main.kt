package me.marolt.configurationserver.web

import com.google.gson.Gson
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import me.marolt.configurationserver.api.IConfigurationContentParser
import me.marolt.configurationserver.api.IConfigurationFormatter
import me.marolt.configurationserver.api.IConfigurationLoader
import me.marolt.configurationserver.api.IPlugin
import me.marolt.configurationserver.services.ConfigurationProcessingPipeline
import me.marolt.configurationserver.utils.DEVELOPMENT_MODE
import me.marolt.configurationserver.utils.logAndThrow
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.pf4j.CompoundPluginDescriptorFinder
import org.pf4j.DefaultPluginManager
import org.pf4j.ManifestPluginDescriptorFinder
import org.pf4j.PluginDescriptorFinder
import java.io.File
import java.io.FileReader
import java.nio.file.FileSystems
import kotlin.concurrent.thread

val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    if (DEVELOPMENT_MODE) {
        Configurator.setLevel("io.netty", Level.DEBUG)
        Configurator.setLevel("org.eclipse.jetty", Level.DEBUG)
    }

    logger.info { "Initializing plugin system!" }
    logger.info { "Current working directory: ${File(".").absolutePath}!" }

    val path = FileSystems.getDefault().getPath(File("plugins/bin").absolutePath)
    val pluginManager = object : DefaultPluginManager(path) {
        override fun createPluginDescriptorFinder(): PluginDescriptorFinder {
            return CompoundPluginDescriptorFinder()
                    .add(ManifestPluginDescriptorFinder())
        }
    }
    pluginManager.loadPlugins()
    pluginManager.startPlugins()

    logger.info { "Loader plugins: [${pluginManager.getExtensions(IPlugin::class.java).joinToString(", ") { it.id }}]" }

    logger.info { "Preparing pipelines." }
    val pipelineConfigurations: Array<PipelineConfiguration> = Gson().fromJson(FileReader(File("pipelineConfiguration.json")), Array<PipelineConfiguration>::class.java)

    val pipelines: List<ConfigurationProcessingPipeline> = pipelineConfigurations.map { pipelineConfiguration ->
        logger.info { "Configuration pipeline ${pipelineConfiguration.name} is being configured!" }
        val loaders: Set<IConfigurationLoader> = pipelineConfiguration.loaders.map { pluginConfiguration ->
            val plugin = pluginManager.getExtensions(IConfigurationLoader::class.java).singleOrNull{ it.id == pluginConfiguration.id }
            if (plugin == null) logger.logAndThrow(IllegalStateException("No loader plugin with id ${pluginConfiguration.id} found!"))
            plugin!!.configure(pluginConfiguration.options)
            plugin!!
        }.toSet()

        val parsers: Set<IConfigurationContentParser> = pipelineConfiguration.parsers.map { pluginConfiguration ->
            val plugin = pluginManager.getExtensions(IConfigurationContentParser::class.java).singleOrNull { it.id == pluginConfiguration.id }
            if (plugin == null) logger.logAndThrow(IllegalStateException("No parser plugin with id ${pluginConfiguration.id} found!"))
            plugin!!.configure(pluginConfiguration.options)
            plugin!!
        }.toSet()

        val formatters: List<IConfigurationFormatter> = pipelineConfiguration.formatters.map { pluginConfiguration ->
            val plugin = pluginManager.getExtensions(IConfigurationFormatter::class.java).singleOrNull { it.id == pluginConfiguration.id }
            if (plugin == null) logger.logAndThrow(IllegalStateException("No formatter plugin with id ${pluginConfiguration.id} found!"))
            plugin!!.configure(pluginConfiguration.options)
            plugin!!
        }

        ConfigurationProcessingPipeline(pipelineConfiguration.name, loaders.toSet(), parsers.toSet(), formatters)
    }

    logger.info { "Configured pipelines: [${pipelines.joinToString(", "){it.name}}]" }

    pipelines.forEach{ it.run() }

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