package me.marolt.configurationserver.services

import me.marolt.configurationserver.services.loaders.FolderConfigurationLoader
import me.marolt.configurationserver.services.parsers.PropertiesConfigurationContentParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigurationProcessingPipelineTests {
    private val pipeline: ConfigurationProcessingPipeline

    init {
        val loader = FolderConfigurationLoader("./src/test/resources/configurations1")
        val parser = PropertiesConfigurationContentParser()
        pipeline = ConfigurationProcessingPipeline(setOf(loader), setOf(parser), emptyList())
    }

    @Test
    fun simple_pipeline() {
        val results = pipeline.run()

        assertEquals(2, results.size)
    }
}