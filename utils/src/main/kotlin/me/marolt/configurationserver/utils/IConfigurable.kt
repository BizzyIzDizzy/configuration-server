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

package me.marolt.configurationserver.utils

interface IConfigurable {

    val configurableOptions: Set<ConfigurableOption>

    fun configure(options: Map<String, Any>)

    @Throws(IllegalArgumentException::class)
    fun checkOptions(options: Map<String, Any>) {
        val results = mutableSetOf<ConfigurableOption>()

        configurableOptions.forEach {
            if (it.required && !options.containsKey(it.name)) {
                results.add(it)
            }
        }

        if (!results.isEmpty()) {
            throw IllegalArgumentException("Required options [${results.joinToString { it.name }}] should be provided!")
        }
    }
}

data class ConfigurableOption(val name: String, val type: ConfigurableOptionType, val required: Boolean = false)
enum class ConfigurableOptionType {
    StringValue, IntegerValue
}