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

package me.marolt.configurationserver.plugins.parsers

import me.marolt.configurationserver.api.PluginId
import me.marolt.configurationserver.api.PluginType
import me.marolt.configurationserver.api.parser.ConfigurationContentParserBase
import mu.KLogging
import java.util.Properties

class PropertiesConfigurationContentParser : ConfigurationContentParserBase() {
    override val id: PluginId by lazy { PluginId(PluginType.Parser, "properties-parser") }
    override val contentType: String by lazy { "properties" }

    companion object : KLogging()

    override fun parseStringContent(content: String): Map<String, String> {
        val properties = Properties()
        properties.load(content.byteInputStream())
        return properties.entries.map { Pair(it.key.toString(), it.value.toString()) }.toMap()
    }
}