package me.marolt.configurationserver.utils

interface IConfigurable {

    val configurableOptions: Set<ConfigurableOption>

    fun configure(options: Map<String, Any>)

}

data class ConfigurableOption(val name: String, val type: ConfigurableOptionType, val required: Boolean = false)
enum class ConfigurableOptionType {
    String, Integer
}