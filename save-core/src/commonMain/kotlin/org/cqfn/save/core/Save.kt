package org.cqfn.save.core

import com.akuleshov7.ktoml.decoders.DecoderConf
import com.akuleshov7.ktoml.decoders.TomlDecoder
import com.akuleshov7.ktoml.exceptions.KtomlException
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.parsers.node.TomlFile
import com.akuleshov7.ktoml.parsers.node.TomlNode
import kotlinx.serialization.serializer
import okio.Path.Companion.toPath
import org.cqfn.save.core.config.SaveProperties
import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.config.TestConfigSections.FIX
import org.cqfn.save.core.config.TestConfigSections.WARN
import org.cqfn.save.core.config.TestConfigSections.GENERAL
import org.cqfn.save.core.config.TestConfigSections
import org.cqfn.save.core.files.ConfigDetector
import org.cqfn.save.core.logging.isDebugEnabled
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logError
import org.cqfn.save.core.logging.logInfo
import org.cqfn.save.core.logging.logWarn
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.plugin.PluginConfig
import org.cqfn.save.core.plugin.PluginException
import org.cqfn.save.core.result.Crash
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Ignored
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.plugin.warn.WarnPlugin
import org.cqfn.save.plugin.warn.WarnPluginConfig
import org.cqfn.save.plugins.fix.FixPlugin
import org.cqfn.save.plugins.fix.FixPluginConfig

/**
 * @property saveProperties an instance of [SaveProperties]
 */
@Suppress("INLINE_CLASS_CAN_BE_USED")  // todo: remove when there are >1 constructor parameters
class Save(
    private val saveProperties: SaveProperties
) {
    init {
        isDebugEnabled = saveProperties.debug ?: false
    }

    /**
     * Main entrypoint for SAVE framework. Discovers plugins and calls their execution.
     */
    fun performAnalysis() {
        // FixMe: now we work only with the save.toml config and it's hierarchy, but we should work properly here with directories as well
        // constructing the file path to the configuration file
        val fullPathToConfig = saveProperties.testRootPath!!.toPath() / saveProperties.testConfigName!!.toPath()
        // get all toml configs in file system
        val testConfig = ConfigDetector().configFromFile(fullPathToConfig)

        val plugins: List<Plugin> = discoverPluginsAndUpdateTestConfig(testConfig)
            // FixMe: Kirill should add merging of configs here
            // ignoring the section with general configuration as it is not needed to create plugin processors
            .pluginConfigs.filterNot { it is GeneralConfig }
            .map {
                when (it) {
                    is FixPluginConfig -> FixPlugin(testConfig)
                    is WarnPluginConfig -> WarnPlugin(testConfig)
                    else -> throw PluginException("Unknown type <${it::class}> of plugin config was provided")
                }
            }

        logInfo("Discovered plugins: $plugins")
        plugins.forEach { plugin ->
            logInfo("Execute plugin: ${plugin::class.simpleName}")
            plugin.execute().forEach(this::handleResult)
            logInfo("${plugin::class.simpleName} successfully executed!")
        }
    }

    private fun discoverPluginsAndUpdateTestConfig(testConfig: TestConfig): TestConfig {
        val testConfigPath = testConfig.location.toString()
        val parsedTomlConfig = TomlParser(testConfigPath).readAndParseFile()
        parsedTomlConfig.getRealTomlTables().forEach {
            // adding a fake file node to restore the structure and parse only the part of te toml
            val fakeFileNode = TomlFile()
            it.children.forEach {
                fakeFileNode.appendChild(it)
            }

            val sectionName = it.name.toUpperCase()
            val sectionPluginConfig: PluginConfig = when (val configName = TestConfigSections.valueOf(sectionName)) {
                FIX -> createPluginConfig<FixPluginConfig>(testConfigPath, fakeFileNode, sectionName)
                WARN -> createPluginConfig<WarnPluginConfig>(testConfigPath, fakeFileNode, sectionName)
                GENERAL -> createPluginConfig<GeneralConfig>(testConfigPath, fakeFileNode, sectionName)
                else -> throw PluginException("Received unknown plugin section name $configName")
            }

            testConfig.pluginConfigs.add(sectionPluginConfig)
        }

        return testConfig
    }

    private inline fun <reified T> createPluginConfig(
        testConfigPath: String,
        fakeFileNode: TomlNode,
        pluginSectionName: String
    ) =
        try {
            TomlDecoder.decode<T>(
                serializer(),
                fakeFileNode,
                DecoderConf()
            )
        } catch (e: KtomlException) {
            logError(
                "Plugin extraction failed for $testConfigPath and [$pluginSectionName] section." +
                        " This file has incorrect toml format."
            )
            throw e
        }

    @Suppress("WHEN_WITHOUT_ELSE")  // TestResult is a sealed class
    private fun handleResult(testResult: TestResult) {
        val status = testResult.status
        when (status) {
            is Pass -> {
                val passMessage = "Test on resources [${testResult.resources}] has completed successfully"
                status.message?.let { logDebug("$passMessage. $it") } ?: logDebug(passMessage)
            }
            is Fail -> logWarn("Test on resources [${testResult.resources}] has failed: ${status.reason}")
            is Ignored -> logWarn("Test on resources [${testResult.resources}] has been ignored: ${status.reason}")
            is Crash -> logError(
                "Test on resources [${testResult.resources}] has crashed: ${status.throwable.message}." +
                        "Please report an issue at https://github.com/cqfn/save"
            )
        }
        logDebug("Completed test execution for resources [${testResult.resources}]. Additional info: ${testResult.debugInfo}")
    }
}
