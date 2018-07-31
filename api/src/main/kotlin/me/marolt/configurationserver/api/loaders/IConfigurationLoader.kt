package me.marolt.configurationserver.api.loaders

import me.marolt.configurationserver.api.IConfiguration

interface IConfigurationLoader<TOptions> {

    fun loadConfigurations(options: TOptions): Set<IConfiguration>

}