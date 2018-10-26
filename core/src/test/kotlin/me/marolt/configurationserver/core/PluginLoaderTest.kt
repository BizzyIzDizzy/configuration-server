package me.marolt.configurationserver.core

import me.marolt.configurationserver.api.*
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