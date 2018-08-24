package me.marolt.configurationserver.plugins.loaders

import me.marolt.configurationserver.api.ValidConfigurationId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DirectoryConfigurationLoaderTests {
    @Test
    @DisplayName("Load all configurations from a directory and it's subdirectories")
    fun simple_load_configurations_from_directory() {
        val loader = DirectoryConfigurationLoaderPlugin.DirectoryConfigurationLoader()
        loader.configure(mapOf("root.path" to "./src/test/resources/configurations"))
        val results = loader.loadConfigurationContents()

        assertEquals(2, results.size)

        val envConfig = results.singleOrNull { it.id == ValidConfigurationId("env") }
        assertNotNull(envConfig)
        assertEquals("properties", envConfig!!.type)
        assertEquals("api.host=localhost\n" +
"api.port=8080\n" +
"api.url=http://\${getString(\"api.host\")}:\${getString(\"api.port\")}\n" +
"api.description=\${\\\n" +
        "  var p1 = getString('api.url');\\n\\\n" +
        "  'Testing ' + p1 + ' API'\\\n" +
        "}", envConfig.content)

        val projectConfig = results.singleOrNull { it.id == ValidConfigurationId("env2") }
        assertNotNull(projectConfig)
        assertEquals("properties", projectConfig!!.type)
        assertEquals("""test.api.host=localhost""", projectConfig.content)
    }
}