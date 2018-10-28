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

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import me.marolt.configurationserver.api.PluginId
import me.marolt.configurationserver.api.PluginType.Parser
import me.marolt.configurationserver.api.parser.ConfigurationContentParserBase
import me.marolt.configurationserver.utils.ConfigurableOption
import me.marolt.configurationserver.utils.ConfigurableOptionType.StringValue
import kotlin.math.roundToInt

class JsonConfigurationContentParser : ConfigurationContentParserBase() {
    private var nullString = ""

    override val contentType: String by lazy { "json" }
    override val id: PluginId by lazy { PluginId(Parser, "json-parser") }
    override val configurableOptions: Set<ConfigurableOption> by lazy {
        setOf(ConfigurableOption("null-string", StringValue))
    }

    override fun applyOptions(options: Map<String, Any>) {
        if (options.containsKey("null-string")) {
            nullString = options.getValue("null-string").toString()
        }
    }

    private val gson by lazy { Gson() }

    override fun parseStringContent(content: String): Map<String, String> {
        val parsed = gson.fromJson(content, Map::class.java)
        val results = mutableMapOf<String, String>()

        for (entry in parsed) {
            val partialResults = parseEntry(entry)
            results.putAll(partialResults)
        }

        return results
    }

    private fun parseEntry(entry: Map.Entry<*, *>, prefix: String = ""): Map<String, String> {
        return if (entry.value is LinkedTreeMap<*, *>) {
            val results = mutableMapOf<String, String>()
            for (subEntry in (entry.value as LinkedTreeMap<*, *>)) {
                val subResults = parseEntry(subEntry, "$prefix${entry.key}.")
                results.putAll(subResults)
            }
            results
        } else {
            return mapOf("$prefix${entry.key}" to parseValue(entry.value))
        }
    }

    private fun parseValue(value: Any?): String {
        if (value == null) {
            return nullString
        }

        if (value is Double) {
            // check for integers
            if (value.compareTo(value.roundToInt()) == 0) {
                return value.roundToInt().toString()
            }
        }

        return value.toString()
    }
}