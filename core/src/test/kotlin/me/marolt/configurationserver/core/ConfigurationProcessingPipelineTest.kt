//       DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
//                   Version 2, December 2004
//
// Copyright (C) 2004 Sam Hocevar <sam@hocevar.net>
//
// Everyone is permitted to copy and distribute verbatim or modified
// copies of this license document, and changing it is allowed as long
// as the name is changed.
//
//            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
//   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
//
//  0. You just DO WHAT THE FUCK YOU WANT TO.

package me.marolt.configurationserver.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigurationProcessingPipelineTest {

    private val pluginRepository by lazy { PluginRepository("./src/test/resources/plugins", "me.marolt.configurationserver.plugins") }

    @Test
    @DisplayName("Pipeline creation should fail without loaders")
    fun should_fail_on_creation_if_no_loaders() {
        val exception = assertThrows<IllegalStateException> {
            ConfigurationProcessingPipeline.configurePipeline(
                PipelineConfiguration(
                    "fail_without_loaders",
                    emptyList(),
                    emptyList(),
                    emptyList()
                ), pluginRepository
            )
        }

        assertEquals("No loaders were configured!", exception.message)
    }

    @Test
    @DisplayName("Pipeline creation should fail without parsers")
    fun should_fail_on_creation_if_no_parsers() {
        val exception = assertThrows<IllegalStateException> {
            ConfigurationProcessingPipeline.configurePipeline(
                PipelineConfiguration(
                    "fail_without_parsers", listOf(
                        PluginConfiguration("directory-loader", mapOf("root.path" to "/"))
                    ), emptyList(), emptyList()
                ), pluginRepository
            )
        }

        assertEquals("No parsers were configured!", exception.message)
    }

    @Test
    @DisplayName("Pipeline run should fail when no parser is configured for type")
    fun should_fail_when_no_parser_configured_for_type() {
        val pipeline = ConfigurationProcessingPipeline.configurePipeline(
            PipelineConfiguration(
                "fail_when_parsing_wrong_type",
                listOf(PluginConfiguration("directory-loader", mapOf("root.path" to "./src/test/resources/configurations1"))),
                listOf(PluginConfiguration("properties-parser", emptyMap())),
                emptyList()
            ), pluginRepository)

        val exception = assertThrows<IllegalStateException> { pipeline.run() }
        assertEquals("No parser found for 'json'!", exception.message)
    }

    @Test
    @DisplayName("Pipeline run should run when no parser is configured for type if ignoreUnknownTypes is true")
    fun should_run_when_no_parser_configured_for_type_if_ignoreUnknownTypes_true() {
        val pipeline = ConfigurationProcessingPipeline.configurePipeline(
            PipelineConfiguration(
                "fail_when_parsing_wrong_type",
                listOf(PluginConfiguration("directory-loader", mapOf("root.path" to "./src/test/resources/configurations1"))),
                listOf(PluginConfiguration("properties-parser", emptyMap())),
                emptyList(),
                true
            ), pluginRepository)

        val results = pipeline.run()
        assertEquals(0, results.size)
    }

    @Test
    @DisplayName("Pipeline run should load and parse configurations")
    fun should_load_and_parse_configurations() {
        val pipeline = ConfigurationProcessingPipeline.configurePipeline(
            PipelineConfiguration(
                "fail_when_parsing_wrong_type",
                listOf(PluginConfiguration("directory-loader", mapOf("root.path" to "./src/test/resources/configurations2"))),
                listOf(PluginConfiguration("json-parser", mapOf("null-string" to "<null>")), PluginConfiguration("properties-parser", emptyMap())),
                emptyList()
            ), pluginRepository)

        val results = pipeline.run()
        assertEquals(3, results.size)

        val env = results.singleOrNull { it.id == "env" }
        assertNotNull(env)
        assertEquals("localhost", env!!.properties["db.host"])
        assertEquals("5432", env.properties["db.port"])
        assertEquals("<null>", env.properties["db.password"])
        assertEquals(3, env.properties.size)

        val extra = results.singleOrNull { it.id == "extra" }
        assertNotNull(extra)
        assertEquals("testValue", extra!!.properties["test"])
        assertEquals("\${getString('test')}", extra.properties["shouldBeFormatted"])
        assertEquals(2, extra.properties.size)

        val app = results.singleOrNull { it.id == "app" }
        assertNotNull(app)
        assertEquals("localhost", app!!.properties["db.host"])
        assertEquals("5432", app.properties["db.port"])
        assertEquals("real password", app.properties["db.password"])
        assertEquals("testValue", app.properties["test"])
        assertEquals("\${getString('test')}", app.properties["shouldBeFormatted"])
        assertEquals("test2Value", app.properties["test2"])
        assertEquals(6, app.properties.size)
    }

    @Test
    @DisplayName("Pipeline run should load, parse and format configurations")
    fun should_load_parse_and_format_configurations() {
        val pipeline = ConfigurationProcessingPipeline.configurePipeline(
            PipelineConfiguration(
                "fail_when_parsing_wrong_type",
                listOf(PluginConfiguration("directory-loader", mapOf("root.path" to "./src/test/resources/configurations2"))),
                listOf(PluginConfiguration("json-parser", mapOf("null-string" to "<null>")), PluginConfiguration("properties-parser", emptyMap())),
                listOf(PluginConfiguration("javascript-expression-formatter", emptyMap()))
            ), pluginRepository)

        val results = pipeline.run()
        assertEquals(3, results.size)

        val env = results.singleOrNull { it.id == "env" }
        assertNotNull(env)
        assertEquals("localhost", env!!.properties["db.host"])
        assertEquals("5432", env.properties["db.port"])
        assertEquals("<null>", env.properties["db.password"])
        assertEquals(3, env.properties.size)

        val extra = results.singleOrNull { it.id == "extra" }
        assertNotNull(extra)
        assertEquals("testValue", extra!!.properties["test"])
        assertEquals("testValue", extra.properties["shouldBeFormatted"])
        assertEquals(2, extra.properties.size)

        val app = results.singleOrNull { it.id == "app" }
        assertNotNull(app)
        assertEquals("localhost", app!!.properties["db.host"])
        assertEquals("5432", app.properties["db.port"])
        assertEquals("real password", app.properties["db.password"])
        assertEquals("testValue", app.properties["test"])
        assertEquals("testValue", app.properties["shouldBeFormatted"])
        assertEquals("test2Value", app.properties["test2"])
        assertEquals(6, app.properties.size)
    }
}