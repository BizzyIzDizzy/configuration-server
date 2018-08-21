package me.marolt.configurationserver.services.loaders

import me.marolt.configurationserver.api.ValidConfigurationId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FolderConfigurationLoaderTests {

    @Test
    @DisplayName("Load all configurations from a directory and it's subdirectories")
    fun simple_load_configurations_from_directory() {
        val loader = FolderConfigurationLoader("./src/test/resources/configurations1")
        val results = loader.loadConfigurationContents()

        assertEquals(2, results.size)

        val envConfig = results.singleOrNull { it.id == ValidConfigurationId("env") }
        assertNotNull(envConfig)
        assertEquals("properties", envConfig!!.type)
        assertEquals("""api.host=localhost
api.port=8080
api.url=http://${'$'}{properties["api.host"]}:${'$'}{properties["api.port"]}""", envConfig.content)

        val projectConfig = results.singleOrNull { it.id == ValidConfigurationId("testProject/testProject") }
        assertNotNull(projectConfig)
        assertEquals("properties", projectConfig!!.type)
        assertEquals("""configuration.metadata.parents=env
api.host=1.1.1.1
api.port=1234""", projectConfig.content)
    }

}