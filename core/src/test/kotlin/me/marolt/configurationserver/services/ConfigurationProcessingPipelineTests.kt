package me.marolt.configurationserver.services

import me.marolt.configurationserver.api.ValidConfigurationId
import me.marolt.configurationserver.plugins.formatters.JavascriptExpressionFormatterPlugin
import me.marolt.configurationserver.plugins.loaders.DirectoryConfigurationLoaderPlugin
import me.marolt.configurationserver.plugins.parsers.PropertiesConfigurationContentParserPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigurationProcessingPipelineTests {
    private val pipeline: ConfigurationProcessingPipeline

    init {
        val loader1 = DirectoryConfigurationLoaderPlugin.DirectoryConfigurationLoader()
        loader1.configure(mapOf("root.path" to "./src/test/resources/configurations1"))
        val loader2 = DirectoryConfigurationLoaderPlugin.DirectoryConfigurationLoader()
        loader2.configure(mapOf("root.path" to "./src/test/resources/configurations2"))

        val parser = PropertiesConfigurationContentParserPlugin.PropertiesConfigurationContentParser()
        val formatter = JavascriptExpressionFormatterPlugin.JavascriptExpressionFormatter()
        pipeline = ConfigurationProcessingPipeline("Test pipeline", setOf(loader1, loader2), setOf(parser), listOf(formatter))
    }

    @Test
    @DisplayName("Simple pipeline test")
    fun simple_pipeline_test() {
        val results = pipeline.run()

        assertEquals(3, results.size)

        val envConfig = results.singleOrNull { it.typedId == ValidConfigurationId("env") }
        assertNotNull(envConfig)
        val envProps = envConfig!!.properties;
        assertEquals(4, envProps.size)
        assertEquals("localhost", envProps.getValue("api.host"))
        assertEquals("8080", envProps.getValue("api.port"))
        assertEquals("http://localhost:8080", envProps.getValue("api.url"))
        assertEquals("Testing http://localhost:8080 API", envProps.getValue("api.description"))

        val env2Config = results.singleOrNull { it.typedId == ValidConfigurationId("env2") }
        assertNotNull(env2Config)
        val env2Props = env2Config!!.properties;
        assertEquals(1, env2Props.size)
        assertEquals("localhost", env2Props.getValue("test.api.host"))

        val projectConfig = results.singleOrNull { it.typedId == ValidConfigurationId("testProject/testProject") }
        assertNotNull(projectConfig)
        val projectProps = projectConfig!!.properties
        assertEquals(5, projectProps.size)
        assertEquals("1.1.1.1", projectProps.getValue("api.host"))
        assertEquals("localhost", projectProps.getValue("test.api.host"))
        assertEquals("1234", projectProps.getValue("api.port"))
        assertEquals("http://1.1.1.1:1234", projectProps.getValue("api.url"))
        assertEquals("Testing http://1.1.1.1:1234 API", projectProps.getValue("api.description"))
    }
}