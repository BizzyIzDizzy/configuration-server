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

package me.marolt.configurationserver.core

import me.marolt.configurationserver.api.IPlugin
import me.marolt.configurationserver.api.PluginId
import me.marolt.configurationserver.utils.logAndThrow
import mu.KotlinLogging
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import java.io.File
import java.lang.reflect.Modifier
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

class PluginRepository(
    private val pluginRoot: String,
    private val packagePrefix: String
) {

    companion object {
        @PublishedApi
        internal val logger = KotlinLogging.logger {}
    }

    @PublishedApi
    internal val reflections by lazy {
        val pluginDir = File(pluginRoot)
        if (!pluginDir.exists()) {
            logger.logAndThrow(IllegalArgumentException("Directory '$pluginRoot' does not exists!"))
        }

        logger.info { "Checking directory '$pluginRoot' for plugins!" }
        val pluginFiles = pluginDir.listFiles().filter { it.extension == "jar" }

        logger.info { "Found plugin files: ${pluginFiles.joinToString(", ") { it.name }}." }
        val classLoader = URLClassLoader(pluginFiles.map { it.toURI().toURL() }.toTypedArray())

        val instance = Reflections(
            ConfigurationBuilder()
                .setScanners(SubTypesScanner(false))
                .addClassLoader(classLoader)
                .setUrls(ClasspathHelper.forPackage(packagePrefix, classLoader)))

        instance.expandSuperTypes()

        instance
    }

    @PublishedApi
    internal val classPluginCache = ConcurrentHashMap<Class<*>, List<Any>>()

    private val pluginCache = ConcurrentHashMap<PluginId, IPlugin>()

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> loadPlugins(): List<T> {
        return classPluginCache.getOrPut(T::class.java) {
            val results = reflections.getSubTypesOf(IPlugin::class.java)
                .filter { !it.isInterface && !Modifier.isAbstract(it.modifiers) && T::class.java.isAssignableFrom(it) }

            if (results.isEmpty()) logger.logAndThrow(IllegalStateException("No plugins found for '${T::class.java}'!"))

            results.map { it.newInstance() }
        } as List<T>
    }

    fun loadPlugin(id: PluginId): IPlugin {
        return pluginCache.getOrPut(id) {
            loadPlugins<IPlugin>()
                .single { it.id == id }
        }
    }

    @Synchronized
    fun reload() {
        logger.info { "Clearing plugin caches." }
        classPluginCache.clear()
        pluginCache.clear()
    }
}