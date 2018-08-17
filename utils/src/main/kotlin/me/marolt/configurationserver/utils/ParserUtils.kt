package me.marolt.configurationserver.utils

import java.util.*

fun parseVariables(properties: Map<String, String>): Map<String, String> {
    TODO()
}


internal fun parseVariable(currentKey: String, allProperties: Map<String, String>, variableStack: Stack<String>): Map<String, String> {
    TODO()
}

internal fun resolveVariables(value: String): List<String> {
    val variables = mutableListOf<String>()

    var openIndex = -1
    var currentIndex = 0
    while (currentIndex < value.length ) {

        if (value[currentIndex] == '{' && currentIndex > 0 && value[currentIndex - 1] == '$' &&
                (currentIndex - 1 == 0 || value[currentIndex - 2] != '\\')) { // ignore escaped values
            openIndex = currentIndex + 1
        }

        if(value[currentIndex] == '}' && openIndex > 0) {
            variables.add(value.substring(openIndex, currentIndex))
            openIndex = -1
        }

        ++currentIndex
    }

    return variables
}
