package me.marolt.configurationserver.api

import me.marolt.configurationserver.utils.IIdentifiable

sealed class ConfigurationId
data class ValidConfigurationId(override val id: String) : ConfigurationId(), IIdentifiable<String>
object InvalidConfigurationId : ConfigurationId()