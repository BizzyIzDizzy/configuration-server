package me.marolt.configurationserver.services.formatters

import me.marolt.configurationserver.api.Configuration
import me.marolt.configurationserver.api.IConfigurationFormatter
import me.marolt.configurationserver.utils.resolveExpressions
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class ExpressionEvaluationFormatter : IConfigurationFormatter {

    private val engine: ScriptEngine = ScriptEngineManager().getEngineByName("nashorn")
    private val functionEngine: Invocable

    init {
        functionEngine = engine as Invocable

        engine.eval("""
            var formatterClass = Java.type('me.marolt.configurationserver.services.formatters.ExpressionEvaluationFormatter');

            function evaluate(smt) {
                formatterClass.test(smt);
            }
        """.trimIndent())
    }

    companion object {

        @JvmStatic
        fun test() {
            println("test from kotlin!")
        }

    }

    override fun format(config: Configuration): Configuration {
        functionEngine.invokeFunction("evaluate", this)
        val allProperties = config.properties

        val formattedProperties = mutableMapOf<String, String>()
        allProperties.forEach {
            val expressions = it.value.resolveExpressions()
            if (!expressions.isEmpty()) {
                val sb = StringBuilder()
                val entries = expressions.entries.toMutableSet()
                val sorted = entries.sortedBy { entry -> entry.key.first }
                var previous = 0
                sorted.forEach {entry ->
                    sb.append(it.value.substring(previous, entry.key.first))
                    previous = entry.key.last + 1
                }
                sb.append(it.value.substring(previous))

                formattedProperties[it.key] = sb.toString()
            }
        }

        val allFormattedProperties = config.formattedProperties.toMutableList()
        allFormattedProperties.add(formattedProperties)

        return Configuration(config.typedId, config.parents, config.ownProperties, allFormattedProperties)
    }

}