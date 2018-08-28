package me.marolt.configurationserver.api

import me.marolt.configurationserver.utils.IConfigurable
import me.marolt.configurationserver.utils.IIdentifiable
import org.pf4j.ExtensionPoint

interface IPlugin : IConfigurable, IIdentifiable<String>, ExtensionPoint