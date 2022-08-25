package com.saveourtool.save.core

import com.saveourtool.save.core.config.EvaluatedToolConfig
import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.core.files.fs
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.plugin.PluginConfig
import com.saveourtool.save.core.utils.createPluginConfigListFromToml
import com.saveourtool.save.core.utils.mergeWith
import com.saveourtool.save.core.utils.singleIsInstance
import com.saveourtool.save.plugin.warn.WarnPluginConfig
import com.saveourtool.save.plugins.fix.FixPluginConfig

import okio.Path.Companion.toPath

import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress(
    "TOO_LONG_FUNCTION",
    "LOCAL_VARIABLE_EARLY_DECLARATION",
    "LONG_LINE",
)
class MergeConfigsTest {
    private val extraFlagsPattern1 = Regex("// RUN: (.*)")
    private val extraFlagsPattern2 = Regex("## RUN: (.*)")
    private val generalConfig1 = GeneralConfig("", listOf("Tag11", "Tag12"), "Description1", "suiteName1", "Kotlin", listOf("excludedTests: test1"), runConfigPattern = extraFlagsPattern2)
    private val generalConfig2 = GeneralConfig("", listOf("Tag21"), "Description2", "suiteName2", "Kotlin", listOf("excludedTests: test3"), runConfigPattern = extraFlagsPattern1)
    private val generalConfig3 = GeneralConfig("", listOf("Tag21", "Tag31", "Tag32"), "Description2", "suiteName3", "Kotlin", listOf("excludedTests: test5", "includedTests: test6"), runConfigPattern = extraFlagsPattern2)
    private val generalConfig4 = GeneralConfig("", listOf("Tag11", "Tag21"), "Description2", "suiteName4", "Kotlin", listOf("excludedTests: test7"), runConfigPattern = extraFlagsPattern2)
    private val warningsOutputPattern1 = Regex(".*")
    private val warningsOutputPattern2 = Regex("\\w+ - (\\d+)/(\\d+) - (.*)$")
    private val warnConfig1 = WarnPluginConfig("execCmd1", warningsOutputPattern2,
        false, false, 1, 1, 1, 1, 1, 1, 1, 1, 1, false, null)
    private val warnConfig2 = WarnPluginConfig("execCmd2", warningsOutputPattern1,
        true, true, 2, 2, 2, 1, 1, 2, 2, 2, 2, true, null)
    private val warnConfig3 = WarnPluginConfig("execCmd3", warningsOutputPattern2,
        warningTextHasColumn = false, lineCaptureGroup = 3, columnCaptureGroup = 3, messageCaptureGroup = 3,
        fileNameCaptureGroupOut = 3, lineCaptureGroupOut = 3, columnCaptureGroupOut = 3, messageCaptureGroupOut = 3)
    private val warnConfig4 = WarnPluginConfig("execCmd4", warningsOutputPattern2,
        lineCaptureGroup = 4, columnCaptureGroup = 4, messageCaptureGroup = 4,
        fileNameCaptureGroupOut = 4, lineCaptureGroupOut = 4, columnCaptureGroupOut = 4, messageCaptureGroupOut = 4)
    private val fixConfig1 = FixPluginConfig("fixCmd1", "Suffix")
    private val fixConfig2 = FixPluginConfig("fixCmd2")
    private val fixConfig3 = FixPluginConfig("fixCmd3", null)
    private val fixConfig4 = FixPluginConfig("fixCmd4")
    private val evaluatedToolConfig = EvaluatedToolConfig(1, "")

    @Test
    fun `merge general configs`() {
        val config1 = mutableListOf(generalConfig1)
        val config2 = mutableListOf<PluginConfig>(generalConfig2)

        config2.mergeWith(config1)

        assertEquals(1, config2.size)

        val expectedGeneralConfig =
                GeneralConfig("", listOf("Tag11", "Tag12", "Tag21"), "Description2", "suiteName2", "Kotlin", listOf("excludedTests: test3"), runConfigPattern = extraFlagsPattern1)

        val actualGeneralConfig = config2.singleIsInstance<GeneralConfig>()
        assertEquals(expectedGeneralConfig, actualGeneralConfig)
    }

    @Test
    fun `merge two incomplete configs`() {
        val config1 = mutableListOf(generalConfig1, warnConfig1)
        val config2 = mutableListOf<PluginConfig>(generalConfig2)

        config2.mergeWith(config1)

        assertEquals(2, config2.size)

        val expectedGeneralConfig =
                GeneralConfig("", listOf("Tag11", "Tag12", "Tag21"), "Description2", "suiteName2", "Kotlin", listOf("excludedTests: test3"), runConfigPattern = extraFlagsPattern1)

        val actualGeneralConfig = config2.singleIsInstance<GeneralConfig>()
        val actualWarnConfig = config2.singleIsInstance<WarnPluginConfig>()

        assertEquals(expectedGeneralConfig, actualGeneralConfig)
        assertEquals(warnConfig1, actualWarnConfig)
    }

    @Test
    fun `merge two incomplete configs 2`() {
        val config1 = mutableListOf<PluginConfig>()
        val config2 = mutableListOf(generalConfig2, warnConfig1)

        config2.mergeWith(config1)

        assertEquals(2, config2.size)

        val actualGeneralConfig = config2.singleIsInstance<GeneralConfig>()
        val actualWarnConfig = config2.singleIsInstance<WarnPluginConfig>()

        assertEquals(generalConfig2, actualGeneralConfig)
        assertEquals(warnConfig1, actualWarnConfig)
    }

    @Test
    fun `merge two configs with different fields`() {
        val config1 = mutableListOf(generalConfig1, warnConfig2, fixConfig1)
        val config2 = mutableListOf(generalConfig2, warnConfig3, fixConfig2)

        config2.mergeWith(config1)

        assertEquals(3, config2.size)

        val expectedGeneralConfig =
                GeneralConfig("", listOf("Tag11", "Tag12", "Tag21"), "Description2", "suiteName2", "Kotlin", listOf("excludedTests: test3"), runConfigPattern = extraFlagsPattern1)
        val expectedWarnConfig = WarnPluginConfig("execCmd3", warningsOutputPattern2,
            true, false, 3, 3, 3, 1, 1, 3, 3, 3, 3, true, null)
        val expectedFixConfig = FixPluginConfig("fixCmd2", "Suffix")

        val actualGeneralConfig = config2.singleIsInstance<GeneralConfig>()
        val actualWarnConfig = config2.singleIsInstance<WarnPluginConfig>()
        val actualFixConfig = config2.singleIsInstance<FixPluginConfig>()

        assertEquals(expectedGeneralConfig, actualGeneralConfig)
        assertEquals(expectedWarnConfig, actualWarnConfig)
        assertEquals(expectedFixConfig, actualFixConfig)
    }

    @Test
    fun `merge configs with many parents`() {
        val config1 = mutableListOf(generalConfig1, warnConfig1, fixConfig1)
        val config2 = mutableListOf(generalConfig2, warnConfig2, fixConfig2)
        val config3 = mutableListOf(generalConfig3, warnConfig3, fixConfig3)
        val config4 = mutableListOf(generalConfig4, warnConfig4, fixConfig4)

        config1.mergeWith(emptyList())
        config2.mergeWith(config1)
        config3.mergeWith(config2)
        config4.mergeWith(config3)

        assertEquals(3, config4.size)
        val expectedGeneralConfig =
                GeneralConfig("", listOf("Tag11", "Tag12", "Tag21", "Tag31", "Tag32"), "Description2", "suiteName4", "Kotlin", listOf("excludedTests: test7"), runConfigPattern = extraFlagsPattern2)
        val expectedWarnConfig = WarnPluginConfig("execCmd4", warningsOutputPattern2,
            true, false, 4, 4, 4, 1, 1, 4, 4, 4, 4, true, null)
        val expectedFixConfig = FixPluginConfig("fixCmd4", "Suffix")

        val actualGeneralConfig = config4.singleIsInstance<GeneralConfig>()
        val actualWarnConfig = config4.singleIsInstance<WarnPluginConfig>()
        val actualFixConfig = config4.singleIsInstance<FixPluginConfig>()

        assertEquals(expectedGeneralConfig, actualGeneralConfig)
        assertEquals(expectedWarnConfig, actualWarnConfig)
        assertEquals(expectedFixConfig, actualFixConfig)
    }

    @Test
    fun `merge real toml configs with empty execFlag in child`() {
        val toml1 = "src/commonNonJsTest/resources/merge_configs/save.toml"
        val configList1 = createPluginConfigListFromToml(toml1.toPath(), fs)

        val parentGeneralConfig = configList1.singleIsInstance<GeneralConfig>()
        val parentWarnConfig = configList1.singleIsInstance<WarnPluginConfig>()
        assertEquals("echo hello world", parentGeneralConfig.execCmd)
        assertEquals(listOf("Tag"), parentGeneralConfig.tags)
        assertEquals(null, parentWarnConfig.execFlags)

        val toml2 = "src/commonNonJsTest/resources/merge_configs/inner/save.toml"
        val configList2 = createPluginConfigListFromToml(toml2.toPath(), fs)

        val childGeneralConfig = configList2.singleIsInstance<GeneralConfig>()
        val childWarnConfig = configList2.singleIsInstance<WarnPluginConfig>()

        assertEquals(listOf(""), childGeneralConfig.tags)
        assertEquals(null, childWarnConfig.execFlags)

        val testConfig1 = TestConfig(toml1.toPath(), null, evaluatedToolConfig, configList1.toMutableList(), emptyList(), fs)
        val testConfig2 = TestConfig(toml2.toPath(), testConfig1, evaluatedToolConfig, configList2.toMutableList(), emptyList(), fs)

        val mergedTestConfig = testConfig2.mergeConfigWithParent()
        testConfig2.validateAndSetDefaults()

        val mergedGeneralConfig = mergedTestConfig.pluginConfigs.singleIsInstance<GeneralConfig>()
        val mergedWarnConfig = mergedTestConfig.pluginConfigs.singleIsInstance<WarnPluginConfig>()

        assertEquals(listOf("Tag", ""), mergedGeneralConfig.tags)
        // execFlags should be empty, not `"null"`
        assertEquals("", mergedWarnConfig.execFlags)
    }
}
