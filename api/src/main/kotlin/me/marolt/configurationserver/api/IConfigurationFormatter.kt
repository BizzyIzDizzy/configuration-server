package me.marolt.configurationserver.api

interface IConfigurationFormatter {
    fun format(config: Configuration): Configuration
}