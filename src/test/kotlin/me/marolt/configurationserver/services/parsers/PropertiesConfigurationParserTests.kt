package me.marolt.configurationserver.services.parsers

import me.marolt.configurationserver.services.ValidConfigurationId
import me.marolt.configurationserver.services.ValidProjectId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PropertiesConfigurationParserTests  {
    private val parser: IConfigurationParser

    init {
        parser = PropertiesConfigurationParser()
    }

    @Test
    fun parse() {
        val projectId = ValidProjectId(1)
        val rawConfigurations = mapOf(
                ValidConfigurationId("root/environment.variables") to """
                    db.host=localhost
                    db.port=5432
                """.trimIndent(),
                ValidConfigurationId("root/app/test.application") to """
                    configuration.metadata.parents=root/environment.variables
                    db.host=127.0.0.1
                """.trimIndent()
        )

        val results = parser.parse(projectId, rawConfigurations)
        assertEquals(results.size, 2)

        results.forEach {
            println("Properties for configuration with id ${it.id}:")
            it.properties.forEach{
                println("${it.key}=${it.value}")
            }
            println()
        }
    }
}