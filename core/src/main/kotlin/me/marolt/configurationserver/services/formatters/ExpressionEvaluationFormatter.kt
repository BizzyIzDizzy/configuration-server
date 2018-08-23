package me.marolt.configurationserver.services.formatters

import me.marolt.configurationserver.api.Configuration
import me.marolt.configurationserver.api.IConfigurationFormatter
import me.marolt.configurationserver.utils.resolveExpressions
import mu.KotlinLogging
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class ExpressionEvaluationFormatter : IConfigurationFormatter {

    private val logger = KotlinLogging.logger { }

    private val engine: ScriptEngine;
    private val functionEngine: Invocable

    init {
        System.setProperty("nashorn.args", "--language=es6")
        engine = ScriptEngineManager().getEngineByName("nashorn")
        functionEngine = engine as Invocable

        engine.eval("""
            let formatterClass = Java.type('me.marolt.configurationserver.services.formatters.ExpressionEvaluationFormatter');
            let propertyCache = {};

            function reset() {
                propertyCache = {};
            }

            function evaluate(formatter, config, script) {
                var getString = function(key) {
                    if(propertyCache[key] !== undefined) {
                        return propertyCache[key];
                    }
                    var value = formatter.getProperty(key, config);
                    propertyCache[key] = value;
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

        """.trimIndent())
    }

    override fun format(config: Configuration): Configuration {
        functionEngine.invokeFunction("reset")

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