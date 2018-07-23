package me.marolt.configurationserver.services

import me.marolt.configurationserver.api.*

class ConfigurationService {

    suspend fun flushAll(projectId: ProjectId = InvalidProjectId) {
        throw TODO()
    }

    suspend fun flush(projectId: ProjectId, configurationId: ConfigurationId = InvalidConfigurationId) {
        throw TODO()
    }

    suspend fun fetchAll(projectId: ProjectId = InvalidProjectId): Set<IConfiguration> {
        throw TODO()
    }

    suspend fun fetch(projectId: ProjectId, configurationId: ValidConfigurationId): IConfiguration{
        throw TODO()
    }

}