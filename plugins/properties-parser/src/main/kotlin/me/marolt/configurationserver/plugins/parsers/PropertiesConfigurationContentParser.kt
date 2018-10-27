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

package me.marolt.configurationserver.plugins.parsers

import me.marolt.configurationserver.api.Configuration
import me.marolt.configurationserver.api.ConfigurationContent
import me.marolt.configurationserver.api.ConfigurationParent
import me.marolt.configurationserver.api.IConfigurationContentParser
import me.marolt.configurationserver.api.PluginId
import me.marolt.configurationserver.api.PluginType
import me.marolt.configurationserver.api.ValidConfigurationId
import me.marolt.configurationserver.utils.ConfigurableOption
import me.marolt.configurationserver.utils.logAndThrow
import mu.KotlinLogging
import java.util.Properties
import java.util.Stack

class PropertiesConfigurationContentParser : IConfigurationContentParser {
    override val configurableOptions: Set<ConfigurableOption> by lazy { emptySet<ConfigurableOption>() }
    override fun configure(options: Map<String, Any>) {}
    override val id: PluginId by lazy { PluginId(PluginType.Parser, "properties-parser") }
    override val contentType: String by lazy { "properties" }

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun parse(
        current: ConfigurationContent,
        parsed: Set<Configuration>,
        rest: Set<ConfigurationContent>,
        parseStack: Stack<ValidConfigurationId>
    ): Set<Configuration> {
        if (parseStack.contains(current.id)) {
            logger.logAndThrow(IllegalStateException("Configuration loop detected! '${current.id}' is already present in the resolution stack!"))
        }

        val existingConfiguration = parsed.singleOrNull { it.typedId == current.id }
        if (existingConfiguration == null) {
            logger.trace { "Parsing single configuration with id '${current.id}'." }

            val newConfigurations = mutableSetOf<Configuration>()

            val properties = Properties()
            properties.load(current.content.byteInputStream())

            if (properties.containsKey(ConfigurationMetadata.PARENT_CONFIGURATIONS.path)) {
                val parentsString = properties.getProperty(ConfigurationMetadata.PARENT_CONFIGURATIONS.path)
                properties.remove(ConfigurationMetadata.PARENT_CONFIGURATIONS.path)
                logger.trace { "Parent configurations: $parentsString!" }
                val parentStrings = parentsString.split(";").map { it.trim() }
                val parents = mutableSetOf<ConfigurationParent>()
                var order = parentStrings.size
                for (parent in parentStrings) {
                    val parentConfigurationId = ValidConfigurationId(parent)

                    if (parseStack.contains(parentConfigurationId)) {
                        logger.logAndThrow(IllegalStateException("Configuration loop detected! '$parentConfigurationId' is already present in the resolution stack!"))
                    }

                    val existingParentConfiguration = parsed.singleOrNull { parentConfigurationId == it.typedId }
                    if (existingParentConfiguration == null) {
                        logger.trace { "Parsing parent configuration with id $parentConfigurationId" }
                        val parentConfigurationContent = rest.singleOrNull { it.id == parentConfigurationId }

                        if (parentConfigurationContent == null) {
                            val errorMessage =
                                "Could not find configuration content with id ${parentConfigurationId.id}!"
                            logger.error { errorMessage }
                            throw IllegalStateException(errorMessage)
                        }

                        val restWithoutTarget = rest.filter { it.id != parentConfigurationId }.toSet()
                        parseStack.push(current.id)
                        val newlyParsed = parse(parentConfigurationContent, parsed, restWithoutTarget, parseStack)
                        parseStack.pop()
                        newConfigurations.addAll(newlyParsed)
                        logger.trace { "When parsing parent configuration with id: $parentConfigurationId, ${newlyParsed.size} configurations were parsed." }
                        parents.add(
                            ConfigurationParent(
                                newlyParsed.single { it.typedId == parentConfigurationId },
                                order--
                            )
                        )
                    } else {
                        parents.add(ConfigurationParent(existingParentConfiguration, order--))
                    }
                }

                logger.trace { "Done parsing parents - parsing configuration with id ${current.id}." }
                val pairs = properties.entries.map { Pair(it.key.toString(), it.value.toString()) }.toTypedArray()
                newConfigurations.add(Configuration(current.id, parents, mapOf(*pairs)))

                return newConfigurations
            } else {
                logger.trace { "No parent configurations found - continuing with parsing properties for configuration with id ${current.id}." }
                val pairs = properties.entries.map { Pair(it.key.toString(), it.value.toString()) }.toTypedArray()
                return setOf(Configuration(current.id, setOf(), mapOf(*pairs)))
            }
        } else {
            return setOf(existingConfiguration)
        }
    }
}

enum class ConfigurationMetadata(val path: String) {
    PARENT_CONFIGURATIONS("configuration.metadata.parents")
}