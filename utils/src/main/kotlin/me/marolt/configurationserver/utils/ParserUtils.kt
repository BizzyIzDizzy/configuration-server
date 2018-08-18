package me.marolt.configurationserver.utils

import java.util.*

fun parseVariables(properties: Map<String, String>): Map<String, String> {
    TODO()
}


internal fun parseVariable(currentKey: String, allProperties: Map<String, String>, variableStack: Stack<String>): Map<String, String> {
    TODO()
}

internal fun resolveExpressions(value: String): Map<String, Expression> {
    val expressions = mutableMapOf<String, Expression>()

    var openIndex = -1
    var currentIndex = 0
    while (currentIndex < value.length) {

        if (value[currentIndex] == '{' && currentIndex > 0 && value[currentIndex - 1] == '$' &&
                (currentIndex - 1 == 0 || value[currentIndex - 2] != '\\')) { // ignore escaped values
            openIndex = currentIndex + 1
        }

        if (value[currentIndex] == '}' && openIndex > 0) {
            val expression = value.substring(openIndex, currentIndex).trim()
            expressions["\${$expression}"] = parseExpression(expression)
            openIndex = -1
        }

        ++currentIndex
    }

    return expressions
}

private val CONST_REGEX = "^\\d+(\\.\\d+)?".toRegex()

internal fun parseExpression(expr: String): Expression {
    if (expr.matches(CONST_REGEX)) {
        return Constant(expr.toDouble())
    }

    val bracketStack = Stack<Int>()
    var firstExpression: Expression? = null

    for (i in 0 until expr.length) {
        val character = expr[i]

        if (!bracketStack.empty() && character != '(' && character != ')') continue

        when (character) {
            '(' -> {
                bracketStack.push(i)
            }
            ')' -> {
                val openBracket = bracketStack.pop()
                if (bracketStack.empty()) {
                    firstExpression = parseExpression(expr.substring(openBracket + 1, i))
                }
            }
            '+' -> return Sum(firstExpression ?: parseExpression(expr.substring(0, i).trim()),
                    parseExpression(expr.substring(i + 1).trim()))
            '-' -> return Sub(firstExpression ?: parseExpression(expr.substring(0, i).trim()),
                    parseExpression(expr.substring(i + 1).trim()))
            '*' -> return Mul(firstExpression ?: parseExpression(expr.substring(0, i).trim()),
                    parseExpression(expr.substring(i + 1).trim()))
            '/' -> return Div(firstExpression ?: parseExpression(expr.substring(0, i).trim()),
                    parseExpression(expr.substring(i + 1).trim()))
            else -> {
            }
        }
    }

    if (firstExpression == null) return Variable(expr)

    return firstExpression
}

internal fun unresolvedVariables(expr: Expression, variables: Set<String>): Set<String> {
    return when (expr) {
        is Constant -> return emptySet()
        is Variable -> {
            if (variables.contains(expr.name)) {
                emptySet()
            } else {
                setOf(expr.name)
            }
        }
        is TwoOperandsOperation -> {
            val a = unresolvedVariables(expr.a, variables)
            val b = unresolvedVariables(expr.b, variables)

            a.union(b)
        }
    }
}

internal fun evaluateExpression(expr: Expression, variables: Map<String, Double>): Double {
    return when(expr) {
        is Constant -> expr.number
        is Variable -> variables[expr.name] ?: throw IllegalStateException("Variable ${expr.name} should be present in variables map!")
        is Sum -> evaluateExpression(expr.a, variables) + evaluateExpression(expr.b, variables)
        is Sub -> evaluateExpression(expr.a, variables) - evaluateExpression(expr.b, variables)
        is Mul -> evaluateExpression(expr.a, variables) * evaluateExpression(expr.b, variables)
        is Div -> evaluateExpression(expr.a, variables) / evaluateExpression(expr.b, variables)
    }
}

internal sealed class Expression
internal data class Constant(val number: Double) : Expression()
internal data class Variable(val name: String) : Expression()
internal sealed class TwoOperandsOperation(open val a: Expression, open val b: Expression) : Expression()
internal data class Sum(override val a: Expression, override val b: Expression) : TwoOperandsOperation(a, b)
internal data class Sub(override val a: Expression, override val b: Expression) : TwoOperandsOperation(a, b)
internal data class Mul(override val a: Expression, override val b: Expression) : TwoOperandsOperation(a, b)
internal data class Div(override val a: Expression, override val b: Expression) : TwoOperandsOperation(a, b)