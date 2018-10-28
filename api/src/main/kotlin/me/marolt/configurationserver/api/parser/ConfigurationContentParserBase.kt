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

package me.marolt.configurationserver.api.parser

import me.marolt.configurationserver.api.Configuration
import me.marolt.configurationserver.api.ConfigurationContent
import me.marolt.configurationserver.api.ConfigurationParent
import me.marolt.configurationserver.api.PluginBase
import me.marolt.configurationserver.api.ValidConfigurationId
import me.marolt.configurationserver.utils.ConfigurableOption
import me.marolt.configurationserver.utils.logAndThrow
import mu.KLogging
import java.util.Stack

abstract class ConfigurationContentParserBase : PluginBase(), IConfigurationContentParser {
    companion object : KLogging() {
        val CONFIGURATION_METADATA_PATHS by lazy {
            ConfigurationMetadata.values().map { it.path }.toSet()
        }
    }

    override val configurableOptions: Set<ConfigurableOption> by lazy { emptySet<ConfigurableOption>() }

    override fun parse(
        current: ConfigurationContent,
        parsed: Set<Configuration>,
        rest: Set<ConfigurationContent>,
        parseStack: Stack<ValidConfigurationId>,
        parsers: Set<IConfigurationContentParser>,
        ignoreUnknownTypes: Boolean
    ): Set<Configuration> {

        if (parseStack.contains(current.id)) {
            logger.logAndThrow(IllegalStateException("Configuration loop detected! '${current.id}' is already present in the resolution stack!"))
        }

        val existingConfiguration = parsed.singleOrNull { it.typedId == current.id }
        if (existingConfiguration == null) {
            logger.trace { "Parsing single configuration with id '${current.id}'." }

            if (current.type != contentType) {
                val parser = parsers.singleOrNull { it.contentType == current.type }

                if (parser != null) {
                    return parser.parse(current, parsed, rest, parseStack, parsers, ignoreUnknownTypes)
                } else if (!ignoreUnknownTypes) {
                    logger.logAndThrow(IllegalStateException("No parser found for '${current.type}'!"))
                }

                return emptySet()
            }

            val newConfigurations = mutableSetOf<Configuration>()
            val pairs = parseStringContent(current.content)

            if (pairs.containsKey(ConfigurationMetadata.PARENT_CONFIGURATIONS.path)) {
                val parentsString = pairs.getValue(ConfigurationMetadata.PARENT_CONFIGURATIONS.path)
                val pairsWithoutMetadata = pairs.filter { !CONFIGURATION_METADATA_PATHS.contains(it.key) }

                logger.trace { "Parent configurations1: $parentsString!" }
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

                        val restWithoutTarget = rest.asSequence().filter { it.id != parentConfigurationId }.toSet()
                        parseStack.push(current.id)
                        val newlyParsed = parse(parentConfigurationContent, parsed, restWithoutTarget, parseStack, parsers, ignoreUnknownTypes)
                        parseStack.pop()
                        newConfigurations.addAll(newlyParsed)
                        logger.trace { "When parsing parent configuration with id: $parentConfigurationId, ${newlyParsed.size} configurations1 were parsed." }
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
                newConfigurations.add(Configuration(current.id, parents, pairsWithoutMetadata))

                return newConfigurations
            } else {
                logger.trace { "No parent configurations1 found - continuing with parsing properties for configuration with id ${current.id}." }
                return setOf(Configuration(current.id, setOf(), pairs))
            }
        } else {
            return setOf(existingConfiguration)
        }
    }

    abstract fun parseStringContent(content: String): Map<String, String>
}