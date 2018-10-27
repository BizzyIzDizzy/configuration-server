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

import mu.KLogger
import java.io.PrintWriter
import java.io.StringWriter

fun getEnvironmentVariable(name: String): String {
    val value = tryGetEnvironmentVariable(name)
    return value ?: throw IllegalStateException("Environment variable with name $name has no value!")
}

fun tryGetEnvironmentVariable(name: String): String? {
    return System.getenv(name)
}

fun getEnvironmentVariableOrDefault(name: String, defaultValue: String): String {
    val value = tryGetEnvironmentVariable(name)
    return value ?: defaultValue
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

@Throws(Throwable::class)
fun KLogger.logAndThrow(throwable: Throwable) {
    this.error { throwable.message }
    throw throwable
}