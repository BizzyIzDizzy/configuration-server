package me.marolt.configurationserver.api

import me.marolt.configurationserver.utils.IUnique

interface IConfiguration : IUnique<String> {
    override val typedId: ValidConfigurationId
    val properties: Map<String, Any>
}

open class MaterializedConfiguration(
        override val typedId: ValidConfigurationId,
        override val properties: Map<String, Any>) : IConfiguration

data class Configuration(
        override val typedId: ValidConfigurationId,
        private val parents: Set<ConfigurationParent>,
        private val ownProperties: Map<String, Any>) : IConfiguration {

    override val properties: Map<String, Any> by lazy {
        materialize().properties
    }

    private fun materialize(): MaterializedConfiguration {
        val allProperties = mutableMapOf<String, Any>()

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

        return MaterializedConfiguration(typedId, allProperties)
    }
}

data class ConfigurationParent(val config: IConfiguration, val order: Int)