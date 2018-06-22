package me.marolt.configurationserver.services

import me.marolt.configurationserver.utils.IUnique
import me.marolt.configurationserver.utils.IIdentifiable

sealed class ProjectId
data class ValidProjectId(override val id: Int) : ProjectId(), IIdentifiable<Int>
object InvalidProjectId : ProjectId()

data class Project(override val typedId: ValidProjectId, val configurations: List<IConfiguration>) : IUnique<Int>

sealed class ConfigurationId
data class ValidConfigurationId(override val id: String): ConfigurationId(), IIdentifiable<String>
object InvalidConfigurationId: ConfigurationId()

interface IConfiguration : IUnique<String>{
    override val typedId: ValidConfigurationId
    val properties: Map<String, Any>
    val project: Project
}

open class MaterializedConfiguration(
        override val typedId: ValidConfigurationId,
        override val project: Project,
        override val properties: Map<String, Any>) : IConfiguration

data class Configuration(
        override val typedId: ValidConfigurationId,
        override val project: Project,
        private val parents: List<ConfigurationParent>,
        private val ownProperties: Map<String, Any>) : IConfiguration {

    override val properties: Map<String, Any> by lazy {
        materialize().properties
    }

    private fun materialize(): MaterializedConfiguration {
        val allProperties = ownProperties.toMutableMap()

        // override properties with values from parent configuration
        // sort by parent sort order value (from highest to lowest)
        parents.sortedByDescending { it.order }
                .forEach { parent -> parent.config.properties.forEach { key, value ->
                    allProperties[key] = value
                }}

        return MaterializedConfiguration(typedId, project, allProperties)
    }
}

data class ConfigurationParent(val config: Configuration, val order: Int)
