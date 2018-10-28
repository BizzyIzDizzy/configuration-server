//       DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
//                   Version 2, December 2004
//
// Copyright (C) 2004 Sam Hocevar <sam@hocevar.net>
//
// Everyone is permitted to copy and distribute verbatim or modified
// copies of this license document, and changing it is allowed as long
// as the name is changed.
//
//            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
//   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
//
//  0. You just DO WHAT THE FUCK YOU WANT TO.

package me.marolt.configurationserver.plugins.loaders

import me.marolt.configurationserver.api.ConfigurationContent
import me.marolt.configurationserver.api.IConfigurationLoader
import me.marolt.configurationserver.api.PluginBase
import me.marolt.configurationserver.api.PluginId
import me.marolt.configurationserver.api.PluginType
import me.marolt.configurationserver.api.ValidConfigurationId
import me.marolt.configurationserver.utils.ConfigurableOption
import me.marolt.configurationserver.utils.ConfigurableOptionType
import me.marolt.configurationserver.utils.logAndThrow
import mu.KotlinLogging
import java.io.File

class DirectoryConfigurationLoader : PluginBase(), IConfigurationLoader {
    override val id: PluginId by lazy { PluginId(PluginType.Loader, "directory-loader") }
    override val configurableOptions: Set<ConfigurableOption> by lazy {
        setOf(
            ConfigurableOption("root.path", ConfigurableOptionType.StringValue, true)
        )
    }

    override fun applyOptions(options: Map<String, Any>) {
        rootPath = options.getValue("root.path").toString()
        rootAbsolutePath = File(rootPath).absolutePath
    }

    private var rootPath: String? = null
    private var rootAbsolutePath: String? = null

    companion object {
        private val logger = KotlinLogging.logger { }
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
                list.add(
                    ConfigurationContent(
                        ValidConfigurationId(fileRelativePathWithoutExtension),
                        extension,
                        it.readText()
                    )
                )
            }
        }

        return list
    }
}