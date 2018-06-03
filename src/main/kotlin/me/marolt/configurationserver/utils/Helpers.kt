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