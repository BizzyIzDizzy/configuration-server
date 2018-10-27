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

package me.marolt.configurationserver.core

import me.marolt.configurationserver.api.Configuration
import me.marolt.configurationserver.api.ConfigurationContent
import me.marolt.configurationserver.api.IConfiguration
import me.marolt.configurationserver.api.IConfigurationContentParser
import me.marolt.configurationserver.api.IConfigurationFormatter
import me.marolt.configurationserver.api.IConfigurationLoader
import me.marolt.configurationserver.api.PluginId
import me.marolt.configurationserver.api.PluginType
import me.marolt.configurationserver.utils.logAndThrow
import mu.KLogging
import java.util.Stack

class ConfigurationProcessingPipeline private constructor(
    val name: String,
    private val loaders: Set<IConfigurationLoader>,
    private val parsers: Set<IConfigurationContentParser>,
    private val formatters: List<IConfigurationFormatter>
) {

    companion object : KLogging()

    fun configurePipeline(pipelineConfig: PipelineConfiguration, pluginRepo: PluginRepository): ConfigurationProcessingPipeline {
        logger.info { "Configuration pipeline '${pipelineConfig.name}' is being configured!" }

        val loaders = pipelineConfig.loaders.map { pc ->
            val plugin = pluginRepo.loadPlugin(PluginId(PluginType.Loader, pc.id))
            plugin.configure(pc.options)
            plugin as IConfigurationLoader
        }.toSet()

        if (loaders.isEmpty()) logger.logAndThrow(IllegalStateException("No loaders were configured!"))

        val parsers = pipelineConfig.parsers.map { pc ->
            val plugin = pluginRepo.loadPlugin(PluginId(PluginType.Parser, pc.id))
            plugin.configure(pc.options)
            plugin as IConfigurationContentParser
        }.toSet()

        if (parsers.isEmpty()) logger.logAndThrow(IllegalStateException("No parsers were configured!"))

        val formatters = pipelineConfig.formatters.map { pc ->
            val plugin = pluginRepo.loadPlugin(PluginId(PluginType.Formatter, pc.id))
            plugin.configure(pc.options)
            plugin as IConfigurationFormatter
        }

        return ConfigurationProcessingPipeline(pipelineConfig.name, loaders, parsers, formatters)
    }

    fun run(): Set<IConfiguration> {
        logger.debug { "Loading configuration contents with the following loaders: ${loaders.joinToString { it.id.toString() }}" }

        val configurationContents = mutableSetOf<ConfigurationContent>()
        for (loader in loaders) {
            val newConfigurationContents = loader.loadConfigurationContents()
            logger.info { "Loaded ${newConfigurationContents.size} new configuration contents!" }
            configurationContents.addAll(newConfigurationContents)
        }

        logger.info { "Done with loading! Parsing ${configurationContents.size} configuration contents with the following parsers: ${parsers.joinToString { it.id.toString() }}." }
        val parsedConfigurations = mutableSetOf<Configuration>()

        while (!configurationContents.isEmpty()) {
            logger.debug { "Parsing loop entered with ${configurationContents.size} configuration contents!" }

            val configurationContent = configurationContents.first()
            configurationContents.remove(configurationContent)

            logger.info { "Parsing configuration '${configurationContent.id}' of type '${configurationContent.type}'." }
            val parser = parsers.singleOrNull { it.contentType == configurationContent.type }
            if (parser != null) {
                val results = parser.parse(configurationContent, parsedConfigurations, configurationContents, Stack())

                logger.info { "Parsing resulted with the following ${results.size} parsed configurations: ${results.joinToString { it.id }}" }
                results.forEach { config ->
                    val rest = configurationContents.singleOrNull { it.id == config.typedId }
                    if (rest != null) configurationContents.remove(rest)

                    parsedConfigurations.add(config)
                }
            } else {
                logger.logAndThrow(IllegalStateException("No parser found for '${configurationContent.type}'!"))
            }
        }

        if (formatters.isEmpty()) {
            logger.info { "No formatters found. Done processing ${parsedConfigurations.size} configurations." }
            return parsedConfigurations
        }

        val configurations = mutableSetOf<Configuration>()

        logger.info { "Formatting ${parsedConfigurations.size} configurations using the following formatters: ${formatters.joinToString { it.id.toString() }}." }

        for (configuration in parsedConfigurations) {
            var formattedConfiguration = configuration
            for (formatter in formatters) {
                formattedConfiguration = formatter.format(formattedConfiguration)
            }

            configurations.add(formattedConfiguration)
        }

        logger.info { "Done processing ${configurations.size} configurations." }

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