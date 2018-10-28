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

package me.marolt.configurationserver.api.parser

import me.marolt.configurationserver.api.Configuration
import me.marolt.configurationserver.api.ConfigurationContent
import me.marolt.configurationserver.api.IPlugin
import me.marolt.configurationserver.api.ValidConfigurationId
import java.util.Stack

interface IConfigurationContentParser : IPlugin {

    val contentType: String

    fun parse(
        current: ConfigurationContent,
        parsed: Set<Configuration>,
        rest: Set<ConfigurationContent>,
        parseStack: Stack<ValidConfigurationId>,
        parsers: Set<IConfigurationContentParser>,
        ignoreUnknownTypes: Boolean
    ): Set<Configuration>
}

enum class ConfigurationMetadata(val path: String) {
    PARENT_CONFIGURATIONS("configuration.metadata.parents")
}