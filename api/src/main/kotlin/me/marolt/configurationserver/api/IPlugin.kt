package me.marolt.configurationserver.api

import me.marolt.configurationserver.utils.IConfigurable
import me.marolt.configurationserver.utils.IIdentifiable

enum class PluginType {
  Loader,
  Parser,
  Formatter,
}

data class PluginId(val type: PluginType, override val id: String): IIdentifiable<String>

interface IPlugin : IConfigurable, IIdentifiable<PluginId>