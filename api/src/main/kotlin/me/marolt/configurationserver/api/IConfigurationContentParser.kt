package me.marolt.configurationserver.api

import java.util.*

interface IConfigurationContentParser : IPlugin {
    val type: String

    fun parse(current: ConfigurationContent,
              parsed: Set<Configuration>,
              rest: Set<ConfigurationContent>,
              parseStack: Stack<ValidConfigurationId> = Stack()): Set<Configuration>
}