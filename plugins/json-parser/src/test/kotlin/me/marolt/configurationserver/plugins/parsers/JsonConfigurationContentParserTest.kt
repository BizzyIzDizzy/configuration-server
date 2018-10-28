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

package me.marolt.configurationserver.plugins.parsers

import me.marolt.configurationserver.api.ConfigurationContent
import me.marolt.configurationserver.api.ValidConfigurationId
import me.marolt.configurationserver.api.parser.IConfigurationContentParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Stack

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonConfigurationContentParserTest {
    private val parser: IConfigurationContentParser by lazy {
        JsonConfigurationContentParser()
    }

    companion object {
        val parentConfigurationId1 = ValidConfigurationId("root/environment.variables")
        val parentConfigurationId2 = ValidConfigurationId("root/infrastructure.variables")
        val childConfigurationId = ValidConfigurationId("root/app/test.application")
    }

    @Test
    @DisplayName("Simple configuration parsing")
    fun parse_simple() {
        val content = ConfigurationContent(
            parentConfigurationId1, "json",
            """
                {
                    "db": {
                        "host": "localhost",
                        "port": 5432,
                        "password": "1234"
                    },
                    "test": "testValue"
                }
            """.trimIndent()
        )

        val results = parser.parse(content, emptySet(), emptySet(), Stack(), setOf(parser), false)
        assertEquals(1, results.size)

        val configuration = results.singleOrNull { it.typedId == parentConfigurationId1 }
        assertNotNull(configuration)

        assertEquals("localhost", configuration!!.properties["db.host"])
        assertEquals("5432", configuration.properties["db.port"])
        assertEquals("1234", configuration.properties["db.password"])
        assertEquals("testValue", configuration.properties["test"])
        assertEquals(4, configuration.properties.size)
    }

    @Test
    @DisplayName("Simple parent - child configuration parsing")
    fun parse_simple_parent_child() {
        val parentConfigurationContent = ConfigurationContent(
            parentConfigurationId1, "json",
            """
                {
                    "db": {
                        "host": "localhost",
                        "port": 5432
                    }
                }
                """.trimIndent()
        )
        val childConfigurationContent = ConfigurationContent(
            childConfigurationId, "json",
            """
                {
                    "configuration": {
                        "metadata": {
                            "parents": "${parentConfigurationId1.id}"
                        }
                    },
                    "db": {
                        "host": "127.0.0.1"
                    }
                }
                """.trimIndent()
        )

        val results = parser.parse(childConfigurationContent, emptySet(), setOf(parentConfigurationContent), Stack(), setOf(parser), false)
        assertEquals(2, results.size)

        val parent = results.singleOrNull { it.typedId == parentConfigurationId1 }
        val child = results.singleOrNull { it.typedId == childConfigurationId }
        assertNotNull(parent)
        assertNotNull(child)

        assertEquals("localhost", parent!!.properties["db.host"])
        assertEquals("5432", parent.properties["db.port"])
        assertEquals(2, parent.properties.size)

        assertEquals("127.0.0.1", child!!.properties["db.host"])
        assertEquals("5432", child.properties["db.port"])
        assertFalse(child.properties.containsKey("configuration.metadata.parents"))
        assertEquals(2, child.properties.size)
    }

    @Test
    @DisplayName("Multiple parents - child configuration parsing")
    fun parse_multiple_parents_child() {
        val parentConfigurationContent1 = ConfigurationContent(
            parentConfigurationId1, "json",
            """
                {
                    "db": {
                        "host": "localhost",
                        "port": 5432,
                        "password": "123"
                    }
                }
        """.trimIndent()
        )
        val parentConfigurationContent2 = ConfigurationContent(
            parentConfigurationId2, "json",
            """
                {
                    "db": {
                        "host": "127.0.0.1"
                    }
                }
        """.trimIndent()
        )
        val childConfigurationContent = ConfigurationContent(
            childConfigurationId, "json",
            """
                {
                    "configuration": {
                        "metadata": {
                            "parents": "${parentConfigurationId1.id};${parentConfigurationId2.id}"
                        }
                    },
                    "db": {
                        "port": 2345
                    }
                }
        """.trimIndent()
        )

        val results = parser.parse(
            childConfigurationContent,
            emptySet(),
            setOf(parentConfigurationContent1, parentConfigurationContent2),
            Stack(),
            setOf(parser),
            false
        )
        assertEquals(3, results.size)

        val parent1 = results.singleOrNull { it.typedId == parentConfigurationId1 }
        assertNotNull(parent1)
        val parent2 = results.singleOrNull { it.typedId == parentConfigurationId2 }
        assertNotNull(parent2)
        val child = results.singleOrNull { it.typedId == childConfigurationId }
        assertNotNull(child)

        assertEquals("localhost", parent1!!.properties["db.host"])
        assertEquals("5432", parent1.properties["db.port"])
        assertEquals("123", parent1.properties["db.password"])
        assertEquals(3, parent1.properties.size)

        assertEquals("127.0.0.1", parent2!!.properties["db.host"])
        assertEquals(1, parent2.properties.size)

        assertEquals("127.0.0.1", child!!.properties["db.host"])
        assertEquals("2345", child.properties["db.port"])
        assertEquals("123", child.properties["db.password"])
        assertFalse(child.properties.containsKey("configuration.metadata.parents"))
        assertEquals(3, child.properties.size)
    }

    @Test
    @DisplayName("Parent - child loop detected")
    fun parse_dependency_loop() {
        val parentConfigurationContent = ConfigurationContent(
            parentConfigurationId1, "json",
            """
                {
                    "configuration": {
                        "metadata": {
                            "parents": "${childConfigurationId.id}"
                        }
                    }
                }
        """.trimIndent()
        )
        val childConfigurationContent = ConfigurationContent(
            childConfigurationId, "json",
            """
                {
                    "configuration": {
                        "metadata": {
                            "parents": "${parentConfigurationId1.id}"
                        }
                    }
                }
        """.trimIndent()
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            parser.parse(childConfigurationContent, emptySet(), setOf(parentConfigurationContent), Stack(), setOf(parser), false)
        }

        assertEquals(
            "Configuration loop detected! 'ValidConfigurationId(id=root/app/test.application)' is already present in the resolution stack!",
            exception.message
        )
    }

    @Test
    @DisplayName("Missing parent")
    fun parse_missing_parent() {
        val configurationContent = ConfigurationContent(
            childConfigurationId, "json",
            """
                {
                    "configuration": {
                        "metadata": {
                            "parents": "${parentConfigurationId1.id}"
                        }
                    }
                }
        """.trimIndent()
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            parser.parse(configurationContent, emptySet(), emptySet(), Stack(), setOf(parser), false)
        }

        assertEquals(
            "Could not find configuration content with id root/environment.variables!",
            exception.message
        )
    }
}