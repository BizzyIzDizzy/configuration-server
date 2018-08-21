package me.marolt.configurationserver.api

data class ConfigurationContent(val id: ValidConfigurationId, val type: String, val content: String)