package me.marolt.configurationserver.services

import me.marolt.configurationserver.utils.IIdentifiable
import me.marolt.configurationserver.utils.IUnique

sealed class ConfigurationId
data class ValidConfigurationId(override val id: String) : ConfigurationId(), IIdentifiable<String>
object InvalidConfigurationId : ConfigurationId()

interface IConfiguration : IUnique<String> {
    override val typedId: ValidConfigurationId
    val properties: Map<String, Any>
    val projectId: ValidProjectId
    val requiredRoles: Set<ValidUserRole>
}

open class MaterializedConfiguration(
        override val typedId: ValidConfigurationId,
        override val projectId: ValidProjectId,
        override val requiredRoles: Set<ValidUserRole>,
        override val properties: Map<String, Any>) : IConfiguration

data class Configuration(
        override val typedId: ValidConfigurationId,
        override val projectId: ValidProjectId,
        override val requiredRoles: Set<ValidUserRole>,
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

        return MaterializedConfiguration(typedId, projectId, requiredRoles, allProperties)
    }
}

data class ConfigurationParent(val config: IConfiguration, val order: Int)