package me.marolt.configurationserver.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ParserUtilsTests {

    @Test
    @DisplayName("Resolve single expression in a string")
    fun resolve_single_expression() {
        val value = "testing1\${properties.test1}testing2"
        val results = value.resolveExpressions()

        assertEquals(1, results.size)
        val entries = results.entries.toTypedArray()
        assertEquals(IntRange(8, 26), entries[0].key)
        assertEquals("properties.test1", entries[0].value)
    }

    @Test
    @DisplayName("Resolve multiple expressions in a string")
    fun resolve_multiple_expressions() {
        val value = "testing1\${properties.test1 + properties.test2}testing2\${properties.test3}testing3\${properties.test4 * 0.25}testing4"
        val results = value.resolveExpressions()

        assertEquals(3, results.size)
        val entries = results.entries.toTypedArray()
        entries.sortBy { it.key.first }
        assertEquals(IntRange(8, 45), entries[0].key)
        assertEquals("properties.test1 + properties.test2", entries[0].value)

        assertEquals(IntRange(54, 72), entries[1].key)
        assertEquals("properties.test3", entries[1].value)

        assertEquals(IntRange(81, 106), entries[2].key)
        assertEquals("properties.test4 * 0.25", entries[2].value)
    }

    @Test
    @DisplayName("Ignore escaped expressions")
    fun ignore_escaped_expressions() {
        val value = "testing1\${properties.test1}testing2\\\${null}testing3\\\${properties.test2}testing4"
        val results = value.resolveExpressions()

        assertEquals(1, results.size)
        val entries = results.entries.toTypedArray()
        assertEquals(IntRange(8, 26), entries[0].key)
        assertEquals("properties.test1", entries[0].value)
    }

    @Test
    @DisplayName("Ignore curly brackets inside expressions")
    fun ignore_brackets_inside_expressions() {
        val value = "testing1\${{}{{properties.test1}}}testing2"
        val results = value.resolveExpressions()

        assertEquals(1, results.size)
        val entries = results.entries.toTypedArray()
        assertEquals(IntRange(8, 32), entries[0].key)
        assertEquals("{}{{properties.test1}}", entries[0].value)
    }

}