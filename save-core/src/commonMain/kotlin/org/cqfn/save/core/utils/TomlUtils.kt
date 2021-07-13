/**
 * This file contain utils for toml files processing
 */

package org.cqfn.save.core.utils

import org.cqfn.save.core.config.TestConfigSections
import org.cqfn.save.core.logging.logError
import org.cqfn.save.core.plugin.PluginConfig
import org.cqfn.save.core.plugin.PluginException
import org.cqfn.save.plugins.fix.FixPluginConfig

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.deserializeTomlFile
import com.akuleshov7.ktoml.exceptions.KtomlException
import com.akuleshov7.ktoml.parsers.TomlParser
import okio.Path

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer

/**
 * Create the plugin config according section name
 *
 * @param fakeFileNode fake file node to restore the structure and parse only the part of the toml
 * @param pluginSectionName name of plugin section from toml file
 */
@OptIn(ExperimentalSerializationApi::class)
private inline fun <reified T : PluginConfig> Path.createPluginConfig(
    pluginSectionName: String
) = try {
    this.toString()
        .deserializeTomlFile<T>(pluginSectionName, KtomlConf())
        .apply {
            configLocation = this@createPluginConfig
        }
} catch (e: KtomlException) {
    logError(
        "Plugin extraction failed for $this and [$pluginSectionName] section." +
                " This file has incorrect toml format or missing section [$pluginSectionName]." +
                " Valid sections are: ${TestConfigSections.values()}"
    )
    throw e
}

/**
 * Create the list of plugins from toml file with plugin sections
 *
 * @param testConfigPath path to the toml file
 * @return list of plugin configs from toml file
 * @throws PluginException in case of unknown plugin
 */
fun createPluginConfigListFromToml(testConfigPath: Path): List<PluginConfig> =
    TomlParser(KtomlConf())
        .readAndParseFile(testConfigPath.toString())
        .getRealTomlTables()
        .map { testConfigPath.createPluginConfig<FixPluginConfig>(it.name.uppercase()) }
