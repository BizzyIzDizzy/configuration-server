package me.marolt.configurationserver.services.loaders

import me.marolt.configurationserver.api.ConfigurationContent
import me.marolt.configurationserver.api.IConfigurationLoader
import me.marolt.configurationserver.api.ValidConfigurationId
import mu.KotlinLogging
import java.io.File

class FolderConfigurationLoader(private val rootPath: String) : IConfigurationLoader {

    companion object {
        val logger = KotlinLogging.logger {}
    }

    private val rootAbsolutePath: String by lazy {
        File(rootPath).absolutePath
    }

    override fun loadConfigurationContents(): Set<ConfigurationContent> {
        logger.info { "Loading files from $rootAbsolutePath!" }
        val list = mutableSetOf<ConfigurationContent>()

        File(this.rootPath).walkTopDown().forEach {
            if (it.isFile) {
                val extension = it.extension
                val fileRelativePath = it.absolutePath.removePrefix("$rootAbsolutePath/")
                val fileRelativePathWithoutExtension = fileRelativePath.removeSuffix(".$extension")
                logger.info { "Loading content from file '$fileRelativePathWithoutExtension' with type '$extension'!" }
                list.add(ConfigurationContent(ValidConfigurationId(fileRelativePathWithoutExtension), extension, it.readText()))
            }
        }

        return list
    }

}