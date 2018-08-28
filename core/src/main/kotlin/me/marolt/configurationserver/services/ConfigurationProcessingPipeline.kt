package me.marolt.configurationserver.services

import me.marolt.configurationserver.api.*
import me.marolt.configurationserver.utils.logAndThrow
import mu.KotlinLogging
import java.util.*

class ConfigurationProcessingPipeline(
        val name: String,
        private val loaders: Set<IConfigurationLoader>,
        private val parsers: Set<IConfigurationContentParser>,
        private val formatters: List<IConfigurationFormatter>) {

    companion object {
        val logger = KotlinLogging.logger {}
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
                logger.logAndThrow(IllegalStateException("No parser was found for '${configurationContent.type}'!"))
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