package me.marolt.configurationserver.api

interface IConfigurationLoader : IPlugin {

    fun loadConfigurationContents(): Set<ConfigurationContent>

}