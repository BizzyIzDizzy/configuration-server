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

package me.marolt.configurationserver.api

import me.marolt.configurationserver.utils.IUnique

interface IConfiguration : IUnique<String> {
    override val typedId: ValidConfigurationId
    val properties: Map<String, String>
}

open class MaterializedConfiguration(
    override val typedId: ValidConfigurationId,
    override val properties: Map<String, String>
) : IConfiguration

data class Configuration(
    override val typedId: ValidConfigurationId,
    val parents: Set<ConfigurationParent>,
    val ownProperties: Map<String, String>,
    val formattedProperties: List<Map<String, String>> = emptyList()
) : IConfiguration {

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
            it.forEach { entry ->
                allProperties[entry.key] = entry.value
            }
        }

        return MaterializedConfiguration(typedId, allProperties)
    }
}

data class ConfigurationParent(val config: IConfiguration, val order: Int)