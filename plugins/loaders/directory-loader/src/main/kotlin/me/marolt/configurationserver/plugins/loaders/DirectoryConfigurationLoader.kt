package me.marolt.configurationserver.plugins.loaders

import me.marolt.configurationserver.api.ConfigurationContent
import me.marolt.configurationserver.api.IConfigurationLoader
import me.marolt.configurationserver.api.ValidConfigurationId
import me.marolt.configurationserver.utils.ConfigurableOption
import me.marolt.configurationserver.utils.ConfigurableOptionType
import me.marolt.configurationserver.utils.logAndThrow
import mu.KotlinLogging
import org.pf4j.Extension
import org.pf4j.Plugin
import org.pf4j.PluginWrapper
import java.io.File

class DirectoryConfigurationLoaderPlugin(wrapper: PluginWrapper) : Plugin(wrapper) {
    @Extension
    class DirectoryConfigurationLoader : IConfigurationLoader {
        override val configurableOptions: Set<ConfigurableOption> by lazy {
            setOf(
                    ConfigurableOption("root.path", ConfigurableOptionType.String, true)
            )
        }

        private var rootPath: String? = null
        private var rootAbsolutePath: String? = null

        companion object {
            private val logger = KotlinLogging.logger { }
        }

        override fun configure(options: Map<String, Any>) {
            if (!options.containsKey("root.path")) {
                logger.logAndThrow(IllegalArgumentException("Loader needs root.path option to be provided!"))
            }

            rootPath = options.getValue("root.path").toString()
            rootAbsolutePath = File(rootPath).absolutePath
        }

        override fun loadConfigurationContents(): Set<ConfigurationContent> {
            if (rootAbsolutePath == null) {
                logger.logAndThrow(IllegalStateException("Loader is not configured correctly! Missing root.path configuration!"))
            }

            logger.info { "Loading files from $rootAbsolutePath" }
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
}