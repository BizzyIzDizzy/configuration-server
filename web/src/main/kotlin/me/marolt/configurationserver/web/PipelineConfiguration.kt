package me.marolt.configurationserver.web

data class PipelineConfiguration(
        val name: String,
        val loaders: List<PluginConfiguration>,
        val parsers: List<PluginConfiguration>,
        val formatters: List<PluginConfiguration>
)

data class PluginConfiguration(val id: String, val options: Map<String, String>)