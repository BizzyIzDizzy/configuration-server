package me.marolt.configurationserver.core

import me.marolt.configurationserver.api.*
import me.marolt.configurationserver.utils.logAndReturn
import mu.KotlinLogging
import org.pf4j.PluginManager
import java.util.*

class ConfigurationProcessingPipeline(
        val name: String,
        private val loaders: Set<IConfigurationLoader>,
        private val parsers: Set<IConfigurationContentParser>,
        private val formatters: List<IConfigurationFormatter>) {

    companion object {
        private val logger = KotlinLogging.logger {}

        fun configurePipeline(pipelineConfig: PipelineConfiguration, pluginManager: PluginManager): ConfigurationProcessingPipeline {
            logger.info { "Configuration pipeline '${pipelineConfig.name}' is being configured!" }

            val loaders: Set<IConfigurationLoader> = pipelineConfig.loaders.map { pc ->
                val plugin = pluginManager.getExtensions(IConfigurationLoader::class.java).singleOrNull { it.id == pc.id }
                        ?: throw logger.logAndReturn(IllegalStateException("No loader plugin with id '${pc.id}' was found!"))
                plugin.configure(pc.options)
                plugin
            }.toSet()

            if (loaders.isEmpty()) throw logger.logAndReturn(IllegalStateException("No loaders were configured!"))

            val parsers: Set<IConfigurationContentParser> = pipelineConfig.parsers.map { pc ->
                val plugin = pluginManager.getExtensions(IConfigurationContentParser::class.java).singleOrNull { it.id == pc.id }
                ?: throw logger.logAndReturn(IllegalStateException("No parser plugin with id '${pc.id}' was found!"))

                plugin.configure(pc.options)
                plugin
            }.toSet()

            if(parsers.isEmpty()) throw logger.logAndReturn(IllegalStateException("No parsers were configured!"))

            val formatters: List<IConfigurationFormatter> = pipelineConfig.formatters.map { pc ->
                val plugin = pluginManager.getExtensions(IConfigurationFormatter::class.java).singleOrNull { it.id == pc.id }
                ?: throw logger.logAndReturn(IllegalStateException("No formatter plugin with id '${pc.id}' was found!"))

                plugin.configure(pc.options)
                plugin
            }

            return ConfigurationProcessingPipeline(pipelineConfig.name, loaders, parsers, formatters)
        }
    }

    fun run(): Set<IConfiguration> {
        logger.info { "Loading configuration contents from ${loaders.size} loaders!" }
        val configurationContents = mutableSetOf<ConfigurationContent>()

        for (loader in loaders) {
            val newConfigurationContents = loader.loadConfigurationContents()
            logger.info { "Loaded ${newConfigurationContents.size} new configuration contents!" }
            configurationContents.addAll(newConfigurationContents)
        }

        logger.info { "Done with loading! Parsing ${configurationContents.size} configuration contents!" }
        val parsedConfigurations = mutableSetOf<Configuration>()

        while (!configurationContents.isEmpty()) {
            logger.debug { "Parsing loop entered with ${configurationContents.size} configuration contents!" }

            val configurationContent = configurationContents.first()
            configurationContents.remove(configurationContent)

            logger.info { "Parsing configuration '${configurationContent.id}' of type '${configurationContent.type}'" }

            val parser = parsers.singleOrNull { it.type == configurationContent.type }
            if (parser != null) {
                val results = parser.parse(configurationContent, parsedConfigurations, configurationContents, Stack())

                logger.info { "Parsing resulted with the following ${results.size} parsed configurations: ${results.joinToString(", ") { it.id }}." }

                results.forEach { config ->
                    val restEntry = configurationContents.singleOrNull { it.id == config.typedId }
                    if (restEntry != null) configurationContents.remove(restEntry)

                    parsedConfigurations.add(config)
                }
            } else {
                throw logger.logAndReturn(IllegalStateException("No parser was found for '${configurationContent.type}'!"))
            }
        }

        if (formatters.isEmpty()) {
            logger.info { "No formatters. Done processing ${parsedConfigurations.size} configurations!" }
            return parsedConfigurations
        }

        val configurations = mutableSetOf<Configuration>()

        logger.info { "Formatting ${parsedConfigurations.size} configurations using ${formatters.size} formatters!" }

        for (configuration in parsedConfigurations) {
            var formattedConfiguration = configuration

            for (formatter in formatters) {
                formattedConfiguration = formatter.format(formattedConfiguration)
            }

            configurations.add(formattedConfiguration)
        }

        logger.info { "Done processing ${configurations.size} configurations!" }

        return configurations
    }
}

data class PipelineConfiguration(
        val name: String,
        val loaders: List<PluginConfiguration>,
        val parsers: List<PluginConfiguration>,
        val formatters: List<PluginConfiguration>
)

data class PluginConfiguration(val id: String, val options: Map<String, String>)