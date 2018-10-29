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

package me.marolt.configurationserver.server

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import me.marolt.configurationserver.utils.DEVELOPMENT_MODE
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator

suspend fun main() = coroutineScope {
    if (DEVELOPMENT_MODE) {
        Configurator.setLevel("io.netty", Level.DEBUG)
        Configurator.setLevel("org.eclipse.jetty", Level.DEBUG)
    }

    val serverControl = ServerControl()
    val job = async {
        serverControl.start()
    }

    job.await()
}