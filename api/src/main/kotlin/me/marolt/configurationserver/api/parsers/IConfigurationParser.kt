package me.marolt.configurationserver.api.parsers

import me.marolt.configurationserver.api.IConfiguration
import me.marolt.configurationserver.api.ValidConfigurationId

interface IConfigurationParser {

    fun parse(configurationContents: Map<ValidConfigurationId, String>): Set<IConfiguration>

}