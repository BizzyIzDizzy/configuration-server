package me.marolt.configurationserver.services.loaders

import me.marolt.configurationserver.api.IConfiguration

interface IConfigurationLoader<TOptions> {

    fun loadConfigurations(options: TOptions): Set<IConfiguration>

}

abstract class LoaderBase<TOptions> : IConfigurationLoader<TOptions> {

    protected fun parse(content: List<String>): IConfiguration {
        throw TODO()
    }

}