package me.marolt.configurationserver.api

import me.marolt.configurationserver.utils.IUnique

interface IConfiguration : IUnique<String> {
    override val typedId: ValidConfigurationId
    val properties: Map<String, String>
}

open class MaterializedConfiguration(
        override val typedId: ValidConfigurationId,
        override val properties: Map<String, String>) : IConfiguration

data class Configuration(
        override val typedId: ValidConfigurationId,
        val parents: Set<ConfigurationParent>,
        val ownProperties: Map<String, String>,
        val formattedProperties: List<Map<String, String>> = emptyList()) : IConfiguration {

    override val properties: Map<String, String> by lazy {
        materialize().properties
    }

    private fun materialize(): MaterializedConfiguration {
        val allProperties = mutableMapOf<String, String>()

        // override properties with values from parent configuration
        // sort by parent sort order value (from highest to lowest)
        parents.sortedByDescending { it.order }
                .forEach { parent ->
                    parent.config.properties.forEach { key, value ->
                        allProperties[key] = value
                    }
                }

        // child properties override everything
        ownProperties.forEach { allProperties[it.key] = it.value }

        // formatted properties override the last
        formattedProperties.forEach {
            it.forEach {entry ->
                allProperties[entry.key] = entry.value
            }
        }

        return MaterializedConfiguration(typedId, allProperties)
    }
}

data class ConfigurationParent(val config: IConfiguration, val order: Int)