package me.marolt.configurationserver.services.parsers

import me.marolt.configurationserver.api.ValidConfigurationId
import me.marolt.configurationserver.api.parsers.IConfigurationParser
import me.marolt.configurationserver.utils.singleOrDefault
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PropertiesConfigurationParserTests {
    private val parser: IConfigurationParser

    init {
        parser = PropertiesConfigurationParser()
    }

    companion object {
        val parentConfigurationId1 = ValidConfigurationId("root/environment.variables")
        val parentConfigurationId2 = ValidConfigurationId("root/infrastructure.variables")
        val childConfigurationId = ValidConfigurationId("root/app/test.application")
    }

    @Test
    @DisplayName("Simple configuration parsing")
    fun parse_simple() {
        val rawConfigurations = mapOf(parentConfigurationId1
                to """
                    db.host=localhost
                    db.port=5432
                    db.password=1234
                """.trimIndent()
        )

        val results = parser.parse(rawConfigurations)
        assertEquals(results.size, 1)

        val configuration = results.singleOrDefault { it.typedId == parentConfigurationId1 }
        assertNotNull(configuration)

        assertTrue(configuration!!.properties.containsKey("db.host"))
        assertTrue(configuration.properties.containsKey("db.port"))
        assertTrue(configuration.properties.containsKey("db.password"))
        assertEquals(configuration.properties.size, 3)
    }

    @Test
    @DisplayName("Simple parent - child configuration parsing")
    fun parse_simple_parent_child() {
        val rawConfigurations = mapOf(parentConfigurationId1
                to """
                    db.host=localhost
                    db.port=5432
                """.trimIndent(),
                childConfigurationId to """
                    configuration.metadata.parents=${parentConfigurationId1.id}
                    db.host=127.0.0.1
                """.trimIndent()
        )

        val results = parser.parse(rawConfigurations)
        assertEquals(results.size, 2)

        val parent = results.singleOrDefault { it.typedId == parentConfigurationId1 }
        val child = results.singleOrDefault { it.typedId == childConfigurationId }
        assertNotNull(parent)
        assertNotNull(child)

        assertTrue(parent!!.properties.containsKey("db.host"))
        assertTrue(parent.properties.containsKey("db.port"))
        assertEquals(parent.properties.size, 2)

        assertTrue(child!!.properties.containsKey("db.host"))
        assertTrue(child.properties.containsKey("db.port"))
        assertFalse(child.properties.containsKey("configuration.metadata.parents"))
        assertEquals(child.properties.size, 2)

        assertEquals(parent.properties["db.port"], child.properties["db.port"])
        assertNotEquals(parent.properties["db.host"], child.properties["db.host"])
    }

    @Test
    @DisplayName("Multiple parents - child configuration parsing")
    fun parse_multiple_parents_child() {
        val rawConfigurations = mapOf(
                parentConfigurationId1 to
                        """
            db.host=localhost
            db.port=5432
            db.password=123
        """.trimIndent(),
                parentConfigurationId2 to
                        """
            db.host=127.0.0.1
        """.trimIndent(),
                childConfigurationId to
                        """
            configuration.metadata.parents=${parentConfigurationId1.id};${parentConfigurationId2.id}
            db.port=2345
        """.trimIndent())

        val results = parser.parse(rawConfigurations)
        assertEquals(results.size, 3)

        val parent1 = results.singleOrDefault { it.typedId == parentConfigurationId1 }
        assertNotNull(parent1)
        val parent2 = results.singleOrDefault { it.typedId == parentConfigurationId2 }
        assertNotNull(parent2)
        val child = results.singleOrDefault { it.typedId == childConfigurationId }
        assertNotNull(child)

        assertTrue(parent1!!.properties.containsKey("db.host"))
        assertTrue(parent1.properties.containsKey("db.port"))
        assertTrue(parent1.properties.containsKey("db.password"))
        assertEquals(parent1.properties.size, 3)

        assertTrue(parent2!!.properties.containsKey("db.host"))
        assertEquals(parent2.properties.size, 1)

        assertTrue(child!!.properties.containsKey("db.host"))
        assertTrue(child.properties.containsKey("db.port"))
        assertTrue(child.properties.containsKey("db.password"))
        assertEquals(parent1.properties["db.password"], child.properties["db.password"])
        assertEquals(parent2.properties["db.host"], child.properties["db.host"])
        assertEquals(child.properties["db.port"], "2345")
        assertFalse(child.properties.containsKey("configuration.metadata.parents"))
        assertEquals(child.properties.size, 3)
    }

    @Test
    @DisplayName("Parent - child loop detected")
    fun parse_dependency_loop() {
        val rawConfigurations = mapOf(
                parentConfigurationId1 to
                        """
            configuration.metadata.parents=${childConfigurationId.id}
        """.trimIndent(),
                childConfigurationId to
                        """
            configuration.metadata.parents=${parentConfigurationId1.id}
        """.trimIndent())

        val exception = assertThrows(IllegalStateException::class.java) {
            parser.parse(rawConfigurations)
        }

        assertEquals(exception.message, "Configuration loop detected - root/environment.variables - root/environment.variables!")
    }

    @Test
    @DisplayName("Missing parent")
    fun parse_missing_parent() {
        val rawConfigurations = mapOf(
                childConfigurationId to
                        """
            configuration.metadata.parents=${parentConfigurationId1.id}
        """.trimIndent())

        val exception = assertThrows(IllegalStateException::class.java) {
            parser.parse(rawConfigurations)
        }

        assertEquals(exception.message, "Could not find configuration content with id root/environment.variables!")
    }
}