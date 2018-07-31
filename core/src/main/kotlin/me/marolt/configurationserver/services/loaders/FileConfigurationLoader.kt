package me.marolt.configurationserver.services.loaders

import me.marolt.configurationserver.api.IConfiguration
import me.marolt.configurationserver.api.loaders.IConfigurationLoader
import java.io.File

data class FileConfigurationLoaderOptions(val rootPath: String)

abstract class LoaderBase<TOptions> : IConfigurationLoader<TOptions> {

    protected fun parse(content: List<String>): IConfiguration {
        throw TODO()
    }

}

class FileConfigurationLoader : LoaderBase<FileConfigurationLoaderOptions>() {

    override fun loadConfigurations(options: FileConfigurationLoaderOptions): Set<IConfiguration> {
        File(options.rootPath)
                .walkTopDown()
                .map { it.readLines() }

        return setOf()
    }

}