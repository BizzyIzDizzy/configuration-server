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

package me.marolt.configurationserver.core

import me.marolt.configurationserver.api.IConfigurationContentParser
import me.marolt.configurationserver.api.IConfigurationFormatter
import me.marolt.configurationserver.api.IConfigurationLoader
import me.marolt.configurationserver.api.IPlugin
import me.marolt.configurationserver.api.PluginId
import me.marolt.configurationserver.api.PluginType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PluginLoaderTest {

    @Test
    fun load_all_plugins_in_directory() {
        val pluginRepository = PluginRepository("./src/test/resources", "me.marolt.configurationserver.plugins")
        val results = pluginRepository.loadPlugins<IPlugin>()

        Assertions.assertEquals(3, results.size)

        var plugin = pluginRepository.loadPlugin(PluginId(PluginType.Loader, "directory-loader"))
        Assertions.assertEquals(PluginId(PluginType.Loader, "directory-loader"), plugin.id)

        val loaderPlugin = plugin as IConfigurationLoader
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            loaderPlugin.configure(emptyMap())
        }

        plugin = pluginRepository.loadPlugin(PluginId(PluginType.Parser, "properties-parser"))
        Assertions.assertEquals(PluginId(PluginType.Parser, "properties-parser"), plugin.id)

        val parserPlugin = plugin as IConfigurationContentParser
        parserPlugin.configure(emptyMap())

        plugin = pluginRepository.loadPlugin(PluginId(PluginType.Formatter, "javascript-expression-formatter"))
        Assertions.assertEquals(PluginId(PluginType.Formatter, "javascript-expression-formatter"), plugin.id)

        val formatterPlugin = plugin as IConfigurationFormatter
        formatterPlugin.configure(emptyMap())
    }
}