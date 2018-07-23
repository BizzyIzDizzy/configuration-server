package me.marolt.configurationserver.services.parsers

import me.marolt.configurationserver.api.*
import me.marolt.configurationserver.utils.singleOrDefault
import mu.KotlinLogging
import java.util.*

class PropertiesConfigurationParser : IConfigurationParser {

    companion object {
        val logger = KotlinLogging.logger {}
    }

    override fun parse(
            configurationContents: Map<ValidConfigurationId, String>): Set<IConfiguration> {

        return if (configurationContents.any()) {
            logger.trace { "Parsing ${configurationContents.size} raw configurations." }
            val newlyParsed = mutableSetOf<IConfiguration>()

            configurationContents.forEach { configurationId, content ->
                newlyParsed.addAll(parseSingle(configurationId, content,
                        configurationContents.filter { it.key != configurationId }, newlyParsed))
            }

            newlyParsed
        } else {
            setOf()
        }
    }

    private fun parseSingle(
            configurationId: ValidConfigurationId,
            current: String,
            rest: Map<ValidConfigurationId, String>,
            parsed: Set<IConfiguration>,
            callerConfigurationId: ConfigurationId = InvalidConfigurationId): Set<IConfiguration> {

        val existingConfiguration = parsed.singleOrDefault { it.typedId == configurationId }
        if (existingConfiguration == null) {
            logger.trace { "Parsing single configuration with id $configurationId." }
            val properties = Properties()
            properties.load(current.byteInputStream())

            if (properties.containsKey(ConfigurationMetadata.PARENT_CONFIGURATIONS.path)) {
                val applicableParsed = parsed.toMutableSet()

                val parentsString = properties.getProperty(ConfigurationMetadata.PARENT_CONFIGURATIONS.path)
                properties.remove(ConfigurationMetadata.PARENT_CONFIGURATIONS.path)
                logger.trace { "Parent configurations: $parentsString!" }
                val parentStrings = parentsString.split(";").map { it.trim() }
                val parents = mutableSetOf<ConfigurationParent>()
                var order = parentStrings.size
                for (parent in parentStrings) {
                    val parentConfigurationId = ValidConfigurationId(parent)

                    if (callerConfigurationId != InvalidConfigurationId) {
                        val errorMessage = "Configuration loop detected - ${parentConfigurationId.id} - ${(callerConfigurationId as ValidConfigurationId).id}!"
                        logger.error { errorMessage }
                        throw IllegalStateException(errorMessage)
                    }

                    val existingParentConfiguration = applicableParsed.singleOrDefault { parentConfigurationId == it.typedId }
                    if (existingParentConfiguration == null) {
                        logger.trace { "Parsing parent configuration with id $parentConfigurationId" }
                        val configurationContent = rest.asIterable().singleOrDefault { it.key == parentConfigurationId }

                        if (configurationContent == null) {
                            val errorMessage = "Could not find configuration content with id ${parentConfigurationId.id}!"
                            logger.error { errorMessage }
                            throw IllegalStateException(errorMessage)
                        }

                        val restWithoutTarget = rest.filter { it.key != parentConfigurationId }
                        val newlyParsed = parseSingle(parentConfigurationId, configurationContent.value, restWithoutTarget, applicableParsed, configurationId)
                        logger.trace { "When parsing parent configuration with id: $parentConfigurationId, ${newlyParsed.size} configurations were parsed." }
                        applicableParsed.addAll(newlyParsed)
                        parents.add(ConfigurationParent(applicableParsed.single { it.typedId == parentConfigurationId }, order--))
                    } else {
                        parents.add(ConfigurationParent(existingParentConfiguration, order--))
                    }
                }

                logger.trace { "Done parsing parents - parsing configuration with id $configurationId." }
                val pairs = properties.entries.map { Pair<String, Any>(it.key.toString(), it.value) }.toTypedArray()
                applicableParsed.add(Configuration(configurationId, parents, mapOf(*pairs)))

                return applicableParsed
            } else {
                logger.trace { "No parent configurations found - continuing with parsing properties for configuration with id $configurationId." }
                val pairs = properties.entries.map { Pair<String, Any>(it.key.toString(), it.value) }.toTypedArray()
                return setOf(Configuration(configurationId, setOf(), mapOf(*pairs)))
            }
        } else {
            return setOf(existingConfiguration)
        }
    }
}