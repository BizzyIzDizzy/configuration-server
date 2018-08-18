package me.marolt.configurationserver.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ParserUtilsTests {

    @Nested
    @DisplayName("Resolving expressions")
    inner class ExpressionResolvingTests {
        @Test
        @DisplayName("Resolve single expression in a string")
        fun resolve_single_expression() {
            val value = "test123\${test.test1.test2}"
            val results = resolveExpressions(value)

            assertEquals(1, results.size)
            val entries = results.entries.toTypedArray()
            assertEquals("\${test.test1.test2}", entries[0].key)
            assertEquals(Variable("test.test1.test2"), entries[0].value)
        }

        @Test
        @DisplayName("Resolve multiple expressions in a string")
        fun resolve_multiple_expressions() {
            val value = "test1\${test.test1}test2\${test.test2}test3\${test.test3}test4"
            val results = resolveExpressions(value)

            assertEquals(3, results.size)
            val entries = results.entries.toTypedArray()
            assertEquals("\${test.test1}", entries[0].key)
            assertEquals(Variable("test.test1"), entries[0].value)

            assertEquals("\${test.test2}", entries[1].key)
            assertEquals(Variable("test.test2"), entries[1].value)

            assertEquals("\${test.test3}", entries[2].key)
            assertEquals(Variable("test.test3"), entries[2].value)
        }

        @Test
        @DisplayName("Ignore escaped expressions")
        fun ignore_escaped_expressions() {
            val value = "test1\${test.test1}test2\\\${test.test2}test3\\\${test.test3}test4"
            val results = resolveExpressions(value)

            assertEquals(1, results.size)
            val entries = results.entries.toTypedArray()
            assertEquals("\${test.test1}", entries[0].key)
            assertEquals(Variable("test.test1"), entries[0].value)
        }
    }

    @Nested
    @DisplayName("Parsing expressions")
    inner class ExpressionParsingTests {
        @Test
        @DisplayName("Parse integer constant")
        fun parse_integer_constant() {
            val value = "123"
            val result = parseExpression(value)

            assertEquals(Constant(123.0), result)
        }

        @Test
        @DisplayName("Parse double constant")
        fun parse_double_constant() {
            val value = "123.456"
            val result = parseExpression(value)

            assertEquals(Constant(123.456), result)
        }

        @Test
        @DisplayName("Parse variable")
        fun parse_variable() {
            val value = "test123"
            val result = parseExpression(value)

            assertEquals(Variable("test123"), result)
        }

        @Test
        @DisplayName("Parse addition")
        fun parse_sum() {
            val value = "test123+ 123.456"
            val result = parseExpression(value)

            assertEquals(Sum(Variable("test123"), Constant(123.456)), result)
        }

        @Test
        @DisplayName("Parse subtraction")
        fun parse_sub() {
            val value = "test123 - 123.456"
            val result = parseExpression(value)

            assertEquals(Sub(Variable("test123"), Constant(123.456)), result)
        }

        @Test
        @DisplayName("Parse multiplication")
        fun parse_mul() {
            val value = "test123 *123.456"
            val result = parseExpression(value)

            assertEquals(Mul(Variable("test123"), Constant(123.456)), result)
        }

        @Test
        @DisplayName("Parse division")
        fun parse_div() {
            val value = "test123 / 123.456"
            val result = parseExpression(value)

            assertEquals(Div(Variable("test123"), Constant(123.456)), result)
        }

        @Test
        @DisplayName("Parse complicated expression")
        fun parse_complicated_expression() {
            val value = "test123 * 123.456 - test456 / 789.123"
            val result = parseExpression(value)

            assertEquals(
                    Mul(
                            Variable("test123"),
                            Sub(
                                    Constant(123.456),
                                    Div(
                                            Variable("test456"),
                                            Constant(789.123)
                                    )
                            )
                    ),
                    result
            )
        }

        @Test
        @DisplayName("Parse simple expression with enclosing brackets")
        fun parse_with_brackets() {
            val value = "(test123 * 123.456)"
            val result = parseExpression(value)

            assertEquals(Mul(Variable("test123"), Constant(123.456)), result)
        }

        @Test
        @DisplayName("Parse expression with multiple brackets")
        fun parse_with_multiple_brackets() {
            val value = "(test123 * (test456 + 123.456))"
            val result = parseExpression(value)

            assertEquals(
                    Mul(
                            Variable("test123"),
                            Sum(
                                    Variable("test456"),
                                    Constant(123.456)
                            )
                    ),
                    result
            )
        }

        @Test
        @DisplayName("Parse complicated expression with brackets")
        fun parse_complicated_expression_with_brackets() {
            val value = "((test123 * 123.456) - test456) / 789.123"
            val result = parseExpression(value)
            println(result)

            assertEquals(
                    Div(
                            Sub(
                                    Mul(
                                            Variable("test123"),
                                            Constant(123.456)),
                                    Variable("test456")),
                            Constant(789.123)
                    ),
                    result)
        }
    }

    @Nested
    @DisplayName("Evaluating expressions")
    inner class ExpressionEvaluatingTests {

        @Test
        @DisplayName("Detect unresolved variables")
        fun detect_unresolved_variables() {
            val expr = Mul(
                    Variable("test123"),
                    Sub(
                            Constant(123.456),
                            Div(
                                    Variable("test456"),
                                    Sum(
                                            Constant(789.0),
                                            Variable("test789")
                                    )
                            )
                    )
            )
            val resolvedVariables = setOf("test123")

            val unresolvedVariables = unresolvedVariables(expr, resolvedVariables)

            assertEquals(2, unresolvedVariables.size)

            assertTrue(unresolvedVariables.contains("test456"))
            assertTrue(unresolvedVariables.contains("test789"))
        }

        @Test
        @DisplayName("Detect no unresolved variables")
        fun detect_no_unresolved_variables() {
            val expr = Mul(
                    Variable("test123"),
                    Sub(
                            Constant(123.456),
                            Div(
                                    Variable("test456"),
                                    Sum(
                                            Constant(789.0),
                                            Variable("test789")
                                    )
                            )
                    )
            )
            val resolvedVariables = setOf("test123", "test456", "test789")
            val unresolvedVariables = unresolvedVariables(expr, resolvedVariables)

            assertEquals(0, unresolvedVariables.size)
        }

    }

}