package me.marolt.configurationserver.services.parsers

import me.marolt.configurationserver.services.IConfiguration
import me.marolt.configurationserver.services.ValidConfigurationId
import me.marolt.configurationserver.services.ValidProjectId

interface IConfigurationParser {

    fun parse(projectId: ValidProjectId, configurationContents: Map<ValidConfigurationId, String>): Set<IConfiguration>

}