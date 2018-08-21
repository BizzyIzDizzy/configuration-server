package me.marolt.configurationserver.utils

import java.util.*


fun String.resolveExpressions(): Map<IntRange, String> {
    val expressions = mutableMapOf<IntRange, String>()

    val bracketStack = Stack<Int>()
    var currentIndex = 0
    var escape = false
    var dollarSign = false

    while (currentIndex < this.length) {
        val previousCharacterWasDollarSign = dollarSign
        dollarSign = false

        if (!escape) {
            val character = this[currentIndex]
            when (character) {
                '\\' -> {
                    escape = true
                }
                '$' -> {
                    dollarSign = true
                }
                '{' -> {
                    if (previousCharacterWasDollarSign || !bracketStack.empty()) {
                        bracketStack.push(currentIndex)
                    }
                }
                '}' -> {
                    if (!bracketStack.empty()) {
                        val openIndex = bracketStack.pop()

                        if (bracketStack.empty()) {
                            expressions[IntRange(openIndex - 1, currentIndex)] = this.substring(openIndex + 1, currentIndex)
                        }
                    }
                }
            }
        } else {
            escape = false
        }

        ++currentIndex
    }

    return expressions
}