package me.marolt.configurationserver.services

import me.marolt.configurationserver.api.ValidConfigurationId
import me.marolt.configurationserver.services.formatters.ExpressionEvaluationFormatter
import me.marolt.configurationserver.services.loaders.FolderConfigurationLoader
import me.marolt.configurationserver.services.parsers.PropertiesConfigurationContentParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigurationProcessingPipelineTests {
    private val pipeline: ConfigurationProcessingPipeline

    init {
        val loader1 = FolderConfigurationLoader("./src/test/resources/configurations1")
        val loader2 = FolderConfigurationLoader("./src/test/resources/configurations2")
        val parser = PropertiesConfigurationContentParser()
        val formatter = ExpressionEvaluationFormatter()
        pipeline = ConfigurationProcessingPipeline(setOf(loader1, loader2), setOf(parser), listOf(formatter))
    }

    @Test
    @DisplayName("Simple pipeline test")
    fun simple_pipeline_test() {
        val results = pipeline.run()

        assertEquals(2, results.size)

        val envConfig = results.singleOrNull { it.typedId == ValidConfigurationId("env") }
        assertNotNull(envConfig)
        val envProps = envConfig!!.properties;
        assertEquals(4, envProps.size)
        assertEquals("localhost", envProps.getValue("api.host"))
        assertEquals("8080", envProps.getValue("api.port"))
        assertEquals("http://localhost:8080", envProps.getValue("api.url"))
        assertEquals("Testing http://localhost:8080 API", envProps.getValue("api.description"))

        val projectConfig = results.singleOrNull { it.typedId == ValidConfigurationId("testProject/testProject") }
        assertNotNull(projectConfig)
        val projectProps = projectConfig!!.properties
        assertEquals(4, projectProps.size)
        assertEquals("1.1.1.1", projectProps.getValue("api.host"))
        assertEquals("1234", projectProps.getValue("api.port"))
        assertEquals("http://1.1.1.1:1234", projectProps.getValue("api.url"))
        assertEquals("Testing http://1.1.1.1:1234 API", projectProps.getValue("api.description"))

    }
}