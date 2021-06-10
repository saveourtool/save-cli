/**
 * This file contain utils for toml files processing
 */

package org.cqfn.save.core.utils

import org.cqfn.save.core.config.TestConfigSections
import org.cqfn.save.core.logging.logError
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.PluginConfig
import org.cqfn.save.core.plugin.PluginException
import org.cqfn.save.plugin.warn.WarnPluginConfig
import org.cqfn.save.plugins.fix.FixPluginConfig

import com.akuleshov7.ktoml.decoders.DecoderConf
import com.akuleshov7.ktoml.decoders.TomlDecoder
import com.akuleshov7.ktoml.exceptions.KtomlException
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.parsers.node.TomlFile
import com.akuleshov7.ktoml.parsers.node.TomlNode
import okio.Path

import kotlinx.serialization.serializer

/**
 * Create the plugin config according section name
 *
 * @param fakeFileNode fake file node to restore the structure and parse only the part of the toml
 * @param pluginSectionName name of plugin section from toml file
 */
private inline fun <reified T : PluginConfig> Path.createPluginConfig(
    fakeFileNode: TomlNode,
    pluginSectionName: String
) =
        try {
            TomlDecoder.decode<T>(
                serializer(),
                fakeFileNode,
                DecoderConf()
            ).apply {
                configLocation = this@createPluginConfig
            }
        } catch (e: KtomlException) {
            logError(
                "Plugin extraction failed for $this and [$pluginSectionName] section." +
                        " This file has incorrect toml format."
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
fun createPluginConfigListFromToml(testConfigPath: Path): MutableList<PluginConfig> {
    val configList: MutableList<PluginConfig> = mutableListOf()
    val parsedTomlConfig = TomlParser(testConfigPath.toString()).readAndParseFile()
    parsedTomlConfig.getRealTomlTables().forEach { tomlPluginSection ->

        // adding a fake file node to restore the structure and parse only the part of the toml
        // this is a hack for easy partial read of Toml configuration
        val fakeFileNode = TomlFile()
        tomlPluginSection.children.forEach {
            fakeFileNode.appendChild(it)
        }

        val sectionName = tomlPluginSection.name.uppercase()
        // we don't convert sectionName to enum, because we don't want to get Kotlin exception
        val sectionPluginConfig = when (sectionName) {
            TestConfigSections.FIX.name -> testConfigPath.createPluginConfig<FixPluginConfig>(
                fakeFileNode,
                sectionName
            )
            TestConfigSections.WARN.name -> testConfigPath.createPluginConfig<WarnPluginConfig>(
                fakeFileNode,
                sectionName
            )
            TestConfigSections.GENERAL.name -> testConfigPath.createPluginConfig<GeneralConfig>(
                fakeFileNode,
                sectionName
            )
            else -> throw PluginException(
                "Received unknown plugin section name in the input: [$sectionName]." +
                        " Please check your <$testConfigPath> config"
            )
        }

        configList.add(sectionPluginConfig)
    }

    return configList
}
