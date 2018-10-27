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

package me.marolt.configurationserver.utils

import java.util.Stack

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
                            expressions[IntRange(openIndex - 1, currentIndex)] =
                                this.substring(openIndex + 1, currentIndex)
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