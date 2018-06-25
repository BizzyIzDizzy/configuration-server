package me.marolt.configurationserver.utils

import java.io.PrintWriter
import java.io.StringWriter

fun getEnvironmentVariable(name: String): String {
    val value = tryGetEnvironmentVariable(name)
    return value ?: throw IllegalStateException("Environment variable with name $name has no value!")
}

fun tryGetEnvironmentVariable(name: String): String? {
    return System.getenv(name)
}

val DEVELOPMENT_MODE = tryGetEnvironmentVariable("CFG_SERVER_DEV_MODE") != null


fun Throwable.fullMessage(): String {
    val sb = StringBuilder()

    sb.append("Message: ${this.localizedMessage}")
    sb.append(System.lineSeparator())
    sb.append(this.stackTraceAsString())

    return sb.toString()
}

fun Throwable.stackTraceAsString(): String {
    StringWriter().use { sw ->
        PrintWriter(sw).use {
            this.printStackTrace(it)
            return sw.toString()
        }
    }
}

inline fun <T> Iterable<T>.singleOrDefault(filter: (T) -> Boolean): T? {
    val filtered = this.filter(filter)
    return when {
        filtered.size > 1 -> throw IllegalStateException("Collection contains more than one element!")
        filtered.isEmpty() -> null
        else -> filtered[0]
    }
}

inline fun <T> Iterable<T>.single(filter: (T) -> Boolean): T {
    return this.singleOrDefault(filter) ?: throw IllegalStateException("Collection does not contain an element matching with provided filter!")
}