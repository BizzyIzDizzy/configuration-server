package me.marolt.configurationserver.services.loaders

import me.marolt.configurationserver.api.IConfiguration
import java.io.File

data class FileConfigurationLoaderOptions(val rootPath: String)

class FileConfigurationLoader : LoaderBase<FileConfigurationLoaderOptions>() {

    override fun loadConfigurations(options: FileConfigurationLoaderOptions): Set<IConfiguration> {
        File(options.rootPath)
                .walkTopDown()
                .map { it.readLines() }

        return setOf()
    }

}