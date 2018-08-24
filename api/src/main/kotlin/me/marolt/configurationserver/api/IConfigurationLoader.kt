package me.marolt.configurationserver.api

import me.marolt.configurationserver.utils.IConfigurable
import org.pf4j.ExtensionPoint

interface IConfigurationLoader : IConfigurable, ExtensionPoint {

    fun loadConfigurationContents(): Set<ConfigurationContent>

}