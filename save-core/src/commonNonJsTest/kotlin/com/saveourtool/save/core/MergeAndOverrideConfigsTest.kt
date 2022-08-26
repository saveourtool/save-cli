package com.saveourtool.save.core

import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.core.config.resolveSaveOverridesTomlConfig
import com.saveourtool.save.core.config.resolveSaveTomlConfig
import com.saveourtool.save.core.files.fs
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.plugin.PluginConfig
import com.saveourtool.save.core.utils.createPluginConfigListFromToml
import com.saveourtool.save.core.utils.mergeWith
import com.saveourtool.save.core.utils.overrideBy
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
class MergeAndOverrideConfigsTest {
    private val extraFlagsPattern1 = Regex("// RUN: (.*)")
    private val extraFlagsPattern2 = Regex("## RUN: (.*)")
    private val generalConfig1 = GeneralConfig("", 1, ", ", listOf("Tag11", "Tag12"), "Description1", "suiteName1", "Kotlin", listOf("excludedTests: test1"), runConfigPattern = extraFlagsPattern2)
    private val generalConfig2 = GeneralConfig("", 1, ", ", listOf("Tag21"), "Description2", "suiteName2", "Kotlin", listOf("excludedTests: test3"), runConfigPattern = extraFlagsPattern1)
    private val generalConfig3 = GeneralConfig("", 1, ", ", listOf("Tag21", "Tag31", "Tag32"), "Description2", "suiteName3", "Kotlin", listOf("excludedTests: test5", "includedTests: test6"), runConfigPattern = extraFlagsPattern2)
    private val generalConfig4 = GeneralConfig("", 1, ", ", listOf("Tag11", "Tag21"), "Description2", "suiteName4", "Kotlin", listOf("excludedTests: test7"), runConfigPattern = extraFlagsPattern2)
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

    @Test
    fun `merge and override general configs`() {
        val config1 = listOf(generalConfig1)

        val config2ForMerge: MutableList<PluginConfig> = mutableListOf(generalConfig2)
        config2ForMerge.mergeWith(config1)
        assertEquals(1, config2ForMerge.size)
        assertEquals(
            GeneralConfig(
                execCmd = "",
                tags = listOf("Tag11", "Tag12", "Tag21"),
                description = "Description2",
                suiteName = "suiteName2",
                language = "Kotlin",
                excludedTests = listOf("excludedTests: test3"),
                runConfigPattern = extraFlagsPattern1
            ),
            config2ForMerge.singleIsInstance()
        )

        val config2ForOverride: MutableList<PluginConfig> = mutableListOf(generalConfig2)
        config2ForOverride.overrideBy(config1)
        assertEquals(1, config2ForOverride.size)
        assertEquals(
            GeneralConfig(
                execCmd = "",
                tags = listOf("Tag21", "Tag11", "Tag12"),
                description = "Description1",
                suiteName = "suiteName1",
                language = "Kotlin",
                excludedTests = listOf("excludedTests: test1"),
                runConfigPattern = extraFlagsPattern2
            ),
            config2ForOverride.singleIsInstance()
        )
    }

    @Test
    fun `merge and override two incomplete configs`() {
        val config1 = listOf(generalConfig1, warnConfig1)

        val config2ForMerge: MutableList<PluginConfig> = mutableListOf(generalConfig2)
        config2ForMerge.mergeWith(config1)
        assertEquals(2, config2ForMerge.size)
        assertEquals(
            GeneralConfig(
                execCmd = "",
                tags = listOf("Tag11", "Tag12", "Tag21"),
                description = "Description2",
                suiteName = "suiteName2",
                language = "Kotlin",
                excludedTests = listOf("excludedTests: test3"),
                runConfigPattern = extraFlagsPattern1
            ),
            config2ForMerge.singleIsInstance()
        )
        assertEquals(warnConfig1, config2ForMerge.singleIsInstance())

        val config2ForOverride: MutableList<PluginConfig> = mutableListOf(generalConfig2)
        config2ForOverride.overrideBy(config1)
        assertEquals(2, config2ForOverride.size)
        assertEquals(
            GeneralConfig(
                execCmd = "",
                tags = listOf("Tag21", "Tag11", "Tag12"),
                description = "Description1",
                suiteName = "suiteName1",
                language = "Kotlin",
                excludedTests = listOf("excludedTests: test1"),
                runConfigPattern = extraFlagsPattern2
            ),
            config2ForOverride.singleIsInstance()
        )
        assertEquals(warnConfig1, config2ForOverride.singleIsInstance())
    }

    @Test
    fun `merge and override two incomplete configs 2`() {
        val config1: List<PluginConfig> = emptyList()

        val config2ForMerge = mutableListOf(generalConfig2, warnConfig1)
        config2ForMerge.mergeWith(config1)
        assertEquals(2, config2ForMerge.size)
        assertEquals(generalConfig2, config2ForMerge.singleIsInstance())
        assertEquals(warnConfig1, config2ForMerge.singleIsInstance())

        val config2ForOverride = mutableListOf(generalConfig2, warnConfig1)
        config2ForOverride.overrideBy(config1)
        assertEquals(2, config2ForOverride.size)
        assertEquals(generalConfig2, config2ForOverride.singleIsInstance())
        assertEquals(warnConfig1, config2ForOverride.singleIsInstance())
    }

    @Test
    fun `merge and override two configs with different fields`() {
        val config1 = listOf(generalConfig1, warnConfig2, fixConfig1)

        val config2ForMerge = mutableListOf(generalConfig2, warnConfig3, fixConfig2)
        config2ForMerge.mergeWith(config1)
        assertEquals(3, config2ForMerge.size)
        assertEquals(
            GeneralConfig(
                execCmd = "",
                tags = listOf("Tag11", "Tag12", "Tag21"),
                description = "Description2",
                suiteName = "suiteName2",
                language = "Kotlin",
                excludedTests = listOf("excludedTests: test3"),
                runConfigPattern = extraFlagsPattern1
            ),
            config2ForMerge.singleIsInstance()
        )
        assertEquals(
            WarnPluginConfig(
                execFlags = "execCmd3",
                actualWarningsPattern = warningsOutputPattern2,
                warningTextHasLine = true,
                warningTextHasColumn = false,
                lineCaptureGroup = 3,
                columnCaptureGroup = 3,
                messageCaptureGroup = 3,
                messageCaptureGroupMiddle = 1,
                messageCaptureGroupEnd = 1,
                fileNameCaptureGroupOut = 3,
                lineCaptureGroupOut = 3,
                columnCaptureGroupOut = 3,
                messageCaptureGroupOut = 3,
                exactWarningsMatch = true,
                testNameRegex = null
            ),
            config2ForMerge.singleIsInstance()
        )
        assertEquals(
            FixPluginConfig("fixCmd2", "Suffix"),
            config2ForMerge.singleIsInstance())

        val config2ForOverride = mutableListOf(generalConfig2, warnConfig3, fixConfig2)
        config2ForOverride.overrideBy(config1)
        assertEquals(3, config2ForOverride.size)
        assertEquals(
            GeneralConfig(
                execCmd = "",
                tags = listOf("Tag21", "Tag11", "Tag12"),
                description = "Description1",
                suiteName = "suiteName1",
                language = "Kotlin",
                excludedTests = listOf("excludedTests: test1"),
                runConfigPattern = extraFlagsPattern2,
            ),
            config2ForOverride.singleIsInstance()
        )
        assertEquals(
            WarnPluginConfig(
                execFlags = "execCmd2",
                actualWarningsPattern = warningsOutputPattern1,
                warningTextHasLine = true,
                warningTextHasColumn = true,
                lineCaptureGroup = 2,
                columnCaptureGroup = 2,
                messageCaptureGroup = 2,
                messageCaptureGroupMiddle = 1,
                messageCaptureGroupEnd = 1,
                fileNameCaptureGroupOut = 2,
                lineCaptureGroupOut = 2,
                columnCaptureGroupOut = 2,
                messageCaptureGroupOut = 2,
                exactWarningsMatch = true,
                testNameRegex = null,
            ),
            config2ForOverride.singleIsInstance()
        )
        assertEquals(
            FixPluginConfig(
                execFlags = "fixCmd1",
                resourceNameTestSuffix = "Suffix",
            ),
            config2ForOverride.singleIsInstance())
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
                GeneralConfig("", 1, ", ", listOf("Tag11", "Tag12", "Tag21", "Tag31", "Tag32"), "Description2", "suiteName4", "Kotlin", listOf("excludedTests: test7"), runConfigPattern = extraFlagsPattern2)
        val expectedWarnConfig = WarnPluginConfig("execCmd4", warningsOutputPattern2,
            true, false, 4, 4, 4, 1, 1, 4, 4, 4, 4, true, null)
        val expectedFixConfig = FixPluginConfig("fixCmd4", "Suffix")

        val actualGeneralConfig: GeneralConfig = config4.singleIsInstance()
        val actualWarnConfig: WarnPluginConfig = config4.singleIsInstance()
        val actualFixConfig: FixPluginConfig = config4.singleIsInstance()

        assertEquals(expectedGeneralConfig, actualGeneralConfig)
        assertEquals(expectedWarnConfig, actualWarnConfig)
        assertEquals(expectedFixConfig, actualFixConfig)
    }

    @Test
    fun `merge real toml configs with empty execFlag in child`() {
        val toml1 = "src/commonNonJsTest/resources/merge_configs/save.toml"
        val configList1 = createPluginConfigListFromToml(toml1.toPath(), fs)

        val parentGeneralConfig: GeneralConfig = configList1.singleIsInstance()
        val parentWarnConfig: WarnPluginConfig = configList1.singleIsInstance()
        assertEquals("echo hello world", parentGeneralConfig.execCmd)
        assertEquals(listOf("Tag"), parentGeneralConfig.tags)
        assertEquals(null, parentWarnConfig.execFlags)

        val toml2 = "src/commonNonJsTest/resources/merge_configs/inner/save.toml"
        val configList2 = createPluginConfigListFromToml(toml2.toPath(), fs)

        val childGeneralConfig: GeneralConfig = configList2.singleIsInstance()
        val childWarnConfig: WarnPluginConfig = configList2.singleIsInstance()

        assertEquals(listOf(""), childGeneralConfig.tags)
        assertEquals(null, childWarnConfig.execFlags)

        val testConfig1 = TestConfig(toml1.toPath(), null, mutableListOf(), emptyList(), fs)
        val testConfig2 = TestConfig(toml2.toPath(), testConfig1, mutableListOf(), emptyList(), fs)

        testConfig1.processInPlace { configList1 }
        testConfig2.processInPlace { configList2 }
        testConfig2.validateAndSetDefaults()

        val mergedGeneralConfig: GeneralConfig = testConfig2.pluginConfigs.singleIsInstance()
        val mergedWarnConfig: WarnPluginConfig = testConfig2.pluginConfigs.singleIsInstance()

        assertEquals(listOf("Tag", ""), mergedGeneralConfig.tags)
        // execFlags should be empty, not `"null"`
        assertEquals("", mergedWarnConfig.execFlags)
    }

    @Test
    fun `override real toml configs`() {
        val saveToml = "src/commonNonJsTest/resources/override_configs".toPath().resolveSaveTomlConfig()
        val configs = createPluginConfigListFromToml(saveToml, fs)

        val saveOverridesToml = "src/commonNonJsTest/resources/override_configs".toPath().resolveSaveOverridesTomlConfig()
        val overrides = createPluginConfigListFromToml(saveOverridesToml, fs)

        val testConfig = TestConfig(
            location = saveToml,
            parentConfig = null,
            pluginConfigs = mutableListOf(),
            overridesPluginConfigs = overrides,
            fs = fs
        )
        testConfig.processInPlace {
            configs
        }
        val result = testConfig.pluginConfigs
        assertEquals(3, result.size)

        assertEquals(
            GeneralConfig(
                execCmd = "java -jar tool.jar",
                tags = listOf("Tag"),
                description = "My description",
                suiteName = "DocsCheck",
            ),
            result.singleIsInstance()
        )
        assertEquals(
            WarnPluginConfig(
                execFlags = "--warn",
            ),
            result.singleIsInstance()
        )
        assertEquals(
            FixPluginConfig(
                execFlags = "--fix",
            ),
            result.singleIsInstance())
    }
}
