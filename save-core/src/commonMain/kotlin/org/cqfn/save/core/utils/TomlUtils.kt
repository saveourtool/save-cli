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
import org.cqfn.save.plugins.fixandwarn.FixAndWarnPluginConfig

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.deserializeTomlFile
import com.akuleshov7.ktoml.exceptions.KtomlException
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.parsers.node.TomlTable
import okio.Path

import kotlinx.serialization.ExperimentalSerializationApi

private fun Path.testConfigFactory(table: TomlTable) =
        when (table.fullTableName.uppercase()) {
            TestConfigSections.FIX.name -> this.createPluginConfig<FixPluginConfig>(
                table.fullTableName
            )
            TestConfigSections.`FIX AND WARN`.name -> this.createPluginConfig<FixAndWarnPluginConfig>(
                table.fullTableName
            )
            TestConfigSections.WARN.name -> this.createPluginConfig<WarnPluginConfig>(
                table.fullTableName
            )
            TestConfigSections.GENERAL.name -> this.createPluginConfig<GeneralConfig>(
                table.fullTableName
            )
            else -> throw PluginException(
                "Received unknown plugin section name in the input: [${table.fullTableName}]." +
                        " Please check your <$this> config"
            )
        }

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
                " Valid sections are: ${TestConfigSections.values().joinToString().lowercase()}."
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
            .getRealTomlTables().filter { !it.fullTableName.contains(".") } // FixMe
            .map { testConfigPath.testConfigFactory(it) }
