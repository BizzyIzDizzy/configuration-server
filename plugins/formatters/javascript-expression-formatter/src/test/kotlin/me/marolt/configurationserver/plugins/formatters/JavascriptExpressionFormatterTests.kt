package me.marolt.configurationserver.plugins.formatters

import me.marolt.configurationserver.api.Configuration
import me.marolt.configurationserver.api.IConfigurationFormatter
import me.marolt.configurationserver.api.ValidConfigurationId
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JavascriptExpressionFormatterTests {

  private val formatter: IConfigurationFormatter

  init {
    formatter = JavascriptExpressionFormatter()
  }

  @Test
  @DisplayName("Find and remove expression from a property")
  fun find_and_remove_expression_from_a_property() {
    val config = Configuration(ValidConfigurationId("test"), emptySet(), mapOf(
      "test.test1" to "testing1\${getString('test.test2')}testing2",
      "test.test2" to "testing1testing2testing3",
      "test.test3" to "testing1\${1+3}testing2\${getInt('test.test6').toString()}testing3",
      "test.test4" to "1",
      "test.test5" to "2",
      "test.test6" to "\${(getInt('test.test4') + getInt('test.test5'))}"
    ))

    val result = formatter.format(config)

    Assertions.assertEquals(1, result.formattedProperties.size)
    val formattedProperties = result.formattedProperties[0]
    Assertions.assertEquals(3, formattedProperties.size)
    val mapEntries = formattedProperties.entries.toTypedArray()

    val first = mapEntries.singleOrNull { it.key == "test.test1" }
    Assertions.assertNotNull(first)
    Assertions.assertEquals("testing1testing1testing2testing3testing2", first!!.value)

    val second = mapEntries.singleOrNull { it.key == "test.test3" }
    Assertions.assertNotNull(second)
    Assertions.assertEquals("testing14testing23testing3", second!!.value)

    val third = mapEntries.singleOrNull { it.key == "test.test6" }
    Assertions.assertNotNull(third)
    Assertions.assertEquals("3.0", third!!.value)
  }
}