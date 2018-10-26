package me.marolt.configurationserver.api

interface IConfigurationFormatter : IPlugin {

  fun format(config: Configuration): Configuration

}