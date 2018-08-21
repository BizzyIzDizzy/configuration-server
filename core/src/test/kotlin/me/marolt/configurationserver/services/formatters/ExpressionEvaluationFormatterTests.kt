package me.marolt.configurationserver.services.formatters

import me.marolt.configurationserver.api.Configuration
import me.marolt.configurationserver.api.IConfigurationFormatter
import me.marolt.configurationserver.api.ValidConfigurationId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExpressionEvaluationFormatterTests {
    private val formatter: IConfigurationFormatter;

    init {
        formatter = ExpressionEvaluationFormatter()
    }

    @Test
    @DisplayName("Find and remove expression from a property")
    fun test() {
        val config = Configuration(ValidConfigurationId("test"), emptySet(), mapOf(
                "test.test1" to "testing1\${properties['test.test2']}testing2",
                "test.test2" to "testing1testing2testing3",
                "test.test3" to "testing1\${blablabla{}}testing2\${{}{{}}}testing3"
        ))

        val result = formatter.format(config)

        assertEquals(1, result.formattedProperties.size)
        val formattedProperties = result.formattedProperties[0]
        assertEquals(2, formattedProperties.size)
        val mapEntries = formattedProperties.entries.toTypedArray()

        val first = mapEntries.singleOrNull { it.key == "test.test1" }
        assertNotNull(first)
        assertEquals("testing1testing2", first!!.value)

        val second = mapEntries.singleOrNull { it.key == "test.test3" }
        assertNotNull(second)
        assertEquals("testing1testing2testing3", second!!.value)
    }
}