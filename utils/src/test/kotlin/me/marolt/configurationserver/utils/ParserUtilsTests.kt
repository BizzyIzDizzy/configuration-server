package me.marolt.configurationserver.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ParserUtilsTests {

    @Test
    @DisplayName("Resolve single variable in a string")
    fun resolve_single_variable() {
        val value = "test123\${test.test1.test2}"
        val results = resolveVariables(value)

        assertEquals(results.size, 1)
        assertEquals(results[0], "test.test1.test2")
    }

    @Test
    @DisplayName("Resolve multiple variables in a string")
    fun resolve_multiple_variables() {
        val value = "test1\${test.test1}test2\${test.test2}test3\${test.test3}test4"
        val results = resolveVariables(value)

        assertEquals(results.size, 3)
        assertEquals(results[0], "test.test1")
        assertEquals(results[1], "test.test2")
        assertEquals(results[2], "test.test3")
    }

    @Test
    @DisplayName("Ignore escaped variables")
    fun ignore_escaped_variables() {
        val value = "test1\${test.test1}test2\\\${test.test2}test3\\\${test.test3}test4"
        val results = resolveVariables(value)

        assertEquals(results.size, 1)
        assertEquals(results[0], "test.test1")
    }
}