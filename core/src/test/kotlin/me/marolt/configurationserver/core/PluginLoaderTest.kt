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

import me.marolt.configurationserver.api.IConfigurationFormatter
import me.marolt.configurationserver.api.IConfigurationLoader
import me.marolt.configurationserver.api.IPlugin
import me.marolt.configurationserver.api.PluginId
import me.marolt.configurationserver.api.PluginType
import me.marolt.configurationserver.api.parser.IConfigurationContentParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PluginLoaderTest {

    @Test
    @DisplayName("PluginRepository should load all plugins in directory")
    fun load_all_plugins_in_directory() {
        val pluginRepository = PluginRepository("./src/test/resources/plugins", "me.marolt.configurationserver.plugins")
        val results = pluginRepository.loadPlugins<IPlugin>()

        assertEquals(4, results.size)

        var plugin = pluginRepository.loadPlugin(PluginId(PluginType.Loader, "file-system-loader"))
        assertEquals(PluginId(PluginType.Loader, "file-system-loader"), plugin.id)

        val loaderPlugin = plugin as IConfigurationLoader
        assertThrows<IllegalArgumentException> {
            loaderPlugin.configure(emptyMap())
        }

        plugin = pluginRepository.loadPlugin(PluginId(PluginType.Parser, "properties-parser"))
        assertEquals(PluginId(PluginType.Parser, "properties-parser"), plugin.id)

        var parserPlugin = plugin as IConfigurationContentParser
        parserPlugin.configure(emptyMap())

        plugin = pluginRepository.loadPlugin(PluginId(PluginType.Parser, "json-parser"))
        assertEquals(PluginId(PluginType.Parser, "json-parser"), plugin.id)

        parserPlugin = plugin as IConfigurationContentParser
        parserPlugin.configure(emptyMap())

        plugin = pluginRepository.loadPlugin(PluginId(PluginType.Formatter, "javascript-expression-formatter"))
        assertEquals(PluginId(PluginType.Formatter, "javascript-expression-formatter"), plugin.id)

        val formatterPlugin = plugin as IConfigurationFormatter
        formatterPlugin.configure(emptyMap())
    }
}