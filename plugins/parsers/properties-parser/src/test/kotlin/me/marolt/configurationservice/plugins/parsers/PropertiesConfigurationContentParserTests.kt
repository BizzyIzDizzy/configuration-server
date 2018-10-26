package me.marolt.configurationservice.plugins.parsers

import me.marolt.configurationserver.api.ConfigurationContent
import me.marolt.configurationserver.api.IConfigurationContentParser
import me.marolt.configurationserver.api.ValidConfigurationId
import me.marolt.configurationserver.plugins.parsers.PropertiesConfigurationContentParser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PropertiesConfigurationParserTests {

  private val parser: IConfigurationContentParser

  init {
    parser = PropertiesConfigurationContentParser()
  }

  companion object {
    val parentConfigurationId1 = ValidConfigurationId("root/environment.variables")
    val parentConfigurationId2 = ValidConfigurationId("root/infrastructure.variables")
    val childConfigurationId = ValidConfigurationId("root/app/test.application")
  }

  @Test
  @DisplayName("Simple configuration parsing")
  fun parse_simple() {
    val content = ConfigurationContent(parentConfigurationId1, "properties",
      """
                    db.host=localhost
                    db.port=5432
                    db.password=1234
                """.trimIndent())

    val results = parser.parse(content, emptySet(), emptySet())
    Assertions.assertEquals(1, results.size)

    val configuration = results.singleOrNull { it.typedId == parentConfigurationId1 }
    Assertions.assertNotNull(configuration)

    Assertions.assertTrue(configuration!!.properties.containsKey("db.host"))
    Assertions.assertTrue(configuration.properties.containsKey("db.port"))
    Assertions.assertTrue(configuration.properties.containsKey("db.password"))
    Assertions.assertEquals(3, configuration.properties.size)
  }

  @Test
  @DisplayName("Simple parent - child configuration parsing")
  fun parse_simple_parent_child() {
    val parentConfigurationContent = ConfigurationContent(parentConfigurationId1, "properties",
      """
                    db.host=localhost
                    db.port=5432
                """.trimIndent())
    val childConfigurationContent = ConfigurationContent(childConfigurationId, "properties",
      """
                    configuration.metadata.parents=${parentConfigurationId1.id}
                    db.host=127.0.0.1
                """.trimIndent())

    val results = parser.parse(childConfigurationContent, emptySet(), setOf(parentConfigurationContent))
    Assertions.assertEquals(2, results.size)

    val parent = results.singleOrNull { it.typedId == parentConfigurationId1 }
    val child = results.singleOrNull { it.typedId == childConfigurationId }
    Assertions.assertNotNull(parent)
    Assertions.assertNotNull(child)

    Assertions.assertTrue(parent!!.properties.containsKey("db.host"))
    Assertions.assertTrue(parent.properties.containsKey("db.port"))
    Assertions.assertEquals(2, parent.properties.size)

    Assertions.assertTrue(child!!.properties.containsKey("db.host"))
    Assertions.assertTrue(child.properties.containsKey("db.port"))
    Assertions.assertFalse(child.properties.containsKey("configuration.metadata.parents"))
    Assertions.assertEquals(2, child.properties.size)

    Assertions.assertEquals(parent.properties["db.port"], child.properties["db.port"])
    Assertions.assertNotEquals(parent.properties["db.host"], child.properties["db.host"])
  }

  @Test
  @DisplayName("Multiple parents - child configuration parsing")
  fun parse_multiple_parents_child() {
    val parentConfigurationContent1 = ConfigurationContent(parentConfigurationId1, "properties",
      """
            db.host=localhost
            db.port=5432
            db.password=123
        """.trimIndent())
    val parentConfigurationContent2 = ConfigurationContent(parentConfigurationId2, "properties",
      """
            db.host=127.0.0.1
        """.trimIndent())
    val childConfigurationContent = ConfigurationContent(childConfigurationId, "properties",
      """
            configuration.metadata.parents=${parentConfigurationId1.id};${parentConfigurationId2.id}
            db.port=2345
        """.trimIndent())

    val results = parser.parse(childConfigurationContent, emptySet(), setOf(parentConfigurationContent1, parentConfigurationContent2))
    Assertions.assertEquals(3, results.size)

    val parent1 = results.singleOrNull { it.typedId == parentConfigurationId1 }
    Assertions.assertNotNull(parent1)
    val parent2 = results.singleOrNull { it.typedId == parentConfigurationId2 }
    Assertions.assertNotNull(parent2)
    val child = results.singleOrNull { it.typedId == childConfigurationId }
    Assertions.assertNotNull(child)

    Assertions.assertTrue(parent1!!.properties.containsKey("db.host"))
    Assertions.assertTrue(parent1.properties.containsKey("db.port"))
    Assertions.assertTrue(parent1.properties.containsKey("db.password"))
    Assertions.assertEquals(3, parent1.properties.size)

    Assertions.assertTrue(parent2!!.properties.containsKey("db.host"))
    Assertions.assertEquals(1, parent2.properties.size)

    Assertions.assertTrue(child!!.properties.containsKey("db.host"))
    Assertions.assertTrue(child.properties.containsKey("db.port"))
    Assertions.assertTrue(child.properties.containsKey("db.password"))
    Assertions.assertEquals(parent1.properties["db.password"], child.properties["db.password"])
    Assertions.assertEquals(parent2.properties["db.host"], child.properties["db.host"])
    Assertions.assertEquals("2345", child.properties["db.port"])
    Assertions.assertFalse(child.properties.containsKey("configuration.metadata.parents"))
    Assertions.assertEquals(3, child.properties.size)
  }

  @Test
  @DisplayName("Parent - child loop detected")
  fun parse_dependency_loop() {
    val parentConfigurationContent = ConfigurationContent(parentConfigurationId1, "properties",
      """
            configuration.metadata.parents=${childConfigurationId.id}
        """.trimIndent())
    val childConfigurationContent = ConfigurationContent(childConfigurationId, "properties",
      """
            configuration.metadata.parents=${parentConfigurationId1.id}
        """.trimIndent())

    val exception = Assertions.assertThrows(IllegalStateException::class.java) {
      parser.parse(childConfigurationContent, emptySet(), setOf(parentConfigurationContent))
    }

    Assertions.assertEquals("Configuration loop detected! 'ValidConfigurationId(id=root/app/test.application)' is already present in the resolution stack!", exception.message)
  }

  @Test
  @DisplayName("Missing parent")
  fun parse_missing_parent() {
    val configurationContent = ConfigurationContent(childConfigurationId, "properties",
      """
            configuration.metadata.parents=${parentConfigurationId1.id}
        """.trimIndent())

    val exception = Assertions.assertThrows(IllegalStateException::class.java) {
      parser.parse(configurationContent, emptySet(), emptySet())
    }

    Assertions.assertEquals("Could not find configuration content with id root/environment.variables!", exception.message)
  }
}