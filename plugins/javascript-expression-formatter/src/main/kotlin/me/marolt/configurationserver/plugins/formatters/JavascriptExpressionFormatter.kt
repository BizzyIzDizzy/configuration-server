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

package me.marolt.configurationserver.plugins.formatters

import me.marolt.configurationserver.api.Configuration
import me.marolt.configurationserver.api.IConfigurationFormatter
import me.marolt.configurationserver.api.PluginBase
import me.marolt.configurationserver.api.PluginId
import me.marolt.configurationserver.api.PluginType
import me.marolt.configurationserver.utils.resolveExpressions
import mu.KotlinLogging
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class JavascriptExpressionFormatter : PluginBase(), IConfigurationFormatter {
    override val id: PluginId by lazy { PluginId(PluginType.Formatter, "javascript-expression-formatter") }

    private val engine: ScriptEngine
    private val functionEngine: Invocable

    init {
        System.setProperty("nashorn.args", "--language=es6")
        engine = ScriptEngineManager().getEngineByName("nashorn")
        functionEngine = engine as Invocable

        engine.eval(
            """
                function evaluate(formatter, config, script) {
                    var getString = function(key) {
                        var value = formatter.getProperty(key, config);
                        return value;
                    };
                    var getInt = function(key) {
                        var value = parseInt(getString(key), 10);
                        return value;
                    };
                    var getFloat = function(key) {
                        var value = parseFloat(getString(key), 10);
                        return value;
                    };
                    return eval(script);
                }

            """.trimIndent()
        )
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun format(config: Configuration): Configuration {
        val allProperties = config.properties

        val formattedProperties = mutableMapOf<String, String>()
        allProperties.forEach {
            val value = formatProperty(it.key, config)
            if (value != null) {
                formattedProperties[it.key] = value
            }
        }

        val allFormattedProperties = config.formattedProperties.toMutableList()
        allFormattedProperties.add(formattedProperties)

        return Configuration(config.typedId, config.parents, config.ownProperties, allFormattedProperties)
    }

    @Suppress("unused")
    fun getProperty(key: String, config: Configuration): String? {
        logger.info { "Getting property '$key'." }
        return formatProperty(key, config) ?: return config.properties.getValue(key)
    }

    private fun formatProperty(key: String, config: Configuration): String? {
        val props = config.properties
        val value = props.getValue(key)
        val expressions = value.resolveExpressions()

        if (expressions.isEmpty()) {
            return null
        }

        logger.info { "Evaluating expressions in property '$key'." }
        val sb = StringBuilder()
        val entries = expressions.entries.sortedBy { it.key.first }

        var previous = 0
        entries.forEach {
            logger.info { "Evaluating expression '${it.value}' for property '$key'." }
            val result = functionEngine.invokeFunction("evaluate", this, config, it.value)
            logger.info { "Evaluating expression '${it.value}' for property '$key' returned '$result' (type ${result.javaClass})." }
            sb.append(value.substring(previous, it.key.first))

            sb.append(result)

            previous = it.key.last + 1
        }
        sb.append(value.substring(previous))

        return sb.toString()
    }
}