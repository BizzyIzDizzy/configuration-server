package me.marolt.configurationserver.api

interface IConfigurationLoader {

    fun loadConfigurationContents(): Set<ConfigurationContent>

}