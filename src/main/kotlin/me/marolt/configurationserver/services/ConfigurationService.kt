package me.marolt.configurationserver.services

import me.marolt.configurationserver.services.dtos.Configuration

class ConfigurationService {

    suspend fun flush(configurationPath: String? = null) {
        throw TODO()
    }

    suspend fun fetch(configurationPath: String): Configuration {
        throw TODO()
    }

}