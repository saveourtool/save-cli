package org.cqfn.save.core

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.utils.createPluginConfigListFromToml
import org.cqfn.save.plugin.warn.WarnPluginConfig
import org.cqfn.save.plugins.fix.FixPluginConfig

import okio.FileSystem
import okio.Path.Companion.toPath

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal val fs = FileSystem.SYSTEM
internal val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / MergeConfigsTest::class.simpleName!!)

internal val toml1 = tmpDir / "save.toml"
internal val nestedDir1 = tmpDir / "nestedDir1"
internal val toml2 = nestedDir1 / "save.toml"
internal val nestedDir2 = tmpDir / "nestedDir1" / "nestedDir2"
internal val toml3 = nestedDir2 / "save.toml"
internal val toml4 = nestedDir2 / "nestedDir3" / "nestedDir4" / "save.toml"

@Suppress("TOO_LONG_FUNCTION", "LOCAL_VARIABLE_EARLY_DECLARATION")
class MergeConfigsTest {
    private val generalConfig1 = GeneralConfig("", listOf("Tag11", "Tag12"), "Description1", "suiteName1", listOf("excludedTests: test1"), listOf("includedTests: test2"))
    private val generalConfig2 = GeneralConfig("", listOf("Tag21"), "Description2", "suiteName2", listOf("excludedTests: test3"), listOf("includedTests: test4"))
    private val generalConfig3 = GeneralConfig("", listOf("Tag21", "Tag31", "Tag32"), "Description2", "suiteName3", listOf("excludedTests: test5", "includedTests: test6"))
    private val generalConfig4 = GeneralConfig("", listOf("Tag11", "Tag21"), "Description2", "suiteName4", listOf("excludedTests: test7"), listOf("includedTests: test8"))
    private val warningsOutputPattern1 = Regex(".*")
    private val warningsOutputPattern2 = Regex("\\w+ - (\\d+)/(\\d+) - (.*)$")
    private val extraFlagsPattern1 = Regex("// RUN: (.*)")
    private val extraFlagsPattern2 = Regex("## RUN: (.*)")
    private val warnConfig1 = WarnPluginConfig("execCmd1", warningsOutputPattern2, extraFlagsPattern2,
        false, false, 1, ", ", 1, 1, 1, 1, 1, 1, 1, false)
    private val warnConfig2 = WarnPluginConfig("execCmd2", warningsOutputPattern1, extraFlagsPattern1,
        true, true, 1, ", ", 2, 2, 2, 2, 2, 2, 2, true)
    private val warnConfig3 = WarnPluginConfig("execCmd3", warningsOutputPattern2, extraFlagsPattern2,
        warningTextHasColumn = false, batchSize = 1, lineCaptureGroup = 3, columnCaptureGroup = 3, messageCaptureGroup = 3,
        fileNameCaptureGroupOut = 3, lineCaptureGroupOut = 3, columnCaptureGroupOut = 3, messageCaptureGroupOut = 3)
    private val warnConfig4 = WarnPluginConfig("execCmd4", warningsOutputPattern2, extraFlagsPattern2,
        batchSize = 1, lineCaptureGroup = 4, columnCaptureGroup = 4, messageCaptureGroup = 4,
        fileNameCaptureGroupOut = 4, lineCaptureGroupOut = 4, columnCaptureGroupOut = 4, messageCaptureGroupOut = 4)
    private val fixConfig1 = FixPluginConfig("fixCmd1", 1, "Suffix")
    private val fixConfig2 = FixPluginConfig("fixCmd2")
    private val fixConfig3 = FixPluginConfig("fixCmd3", null)
    private val fixConfig4 = FixPluginConfig("fixCmd4")

    @Test
    fun `merge general configs`() {
        createTomlFiles()
        val config1 = TestConfig(toml1, null, mutableListOf(generalConfig1), fs)
        val config2 = TestConfig(toml2, config1, mutableListOf(generalConfig2), fs)

        config2.mergeConfigWithParents()

        assertEquals(1, config2.pluginConfigs.size)

        val expectedGeneralConfig =
                GeneralConfig("", listOf("Tag11", "Tag12", "Tag21"), "Description2", "suiteName2", listOf("excludedTests: test3"), listOf("includedTests: test4"))

        val actualGeneralConfig = config2.pluginConfigs.filterIsInstance<GeneralConfig>().first()

        assertEquals(expectedGeneralConfig, actualGeneralConfig)
    }

    @Test
    fun `merge two incomplete configs`() {
        createTomlFiles()
        val config1 = TestConfig(toml1, null, mutableListOf(generalConfig1, warnConfig1), fs)
        val config2 = TestConfig(toml2, config1, mutableListOf(generalConfig2), fs)

        config2.mergeConfigWithParents()

        assertEquals(2, config2.pluginConfigs.size)

        val expectedGeneralConfig =
                GeneralConfig("", listOf("Tag11", "Tag12", "Tag21"), "Description2", "suiteName2", listOf("excludedTests: test3"), listOf("includedTests: test4"))

        val actualGeneralConfig = config2.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        val actualWarnConfig = config2.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()

        assertEquals(expectedGeneralConfig, actualGeneralConfig)
        assertEquals(warnConfig1, actualWarnConfig)
    }

    @Test
    fun `merge two incomplete configs 2`() {
        createTomlFiles()
        val config1 = TestConfig(toml1, null, mutableListOf(), fs)
        val config2 = TestConfig(toml2, config1, mutableListOf(generalConfig2, warnConfig1), fs)

        config2.mergeConfigWithParents()

        assertEquals(2, config2.pluginConfigs.size)

        val actualGeneralConfig = config2.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        val actualWarnConfig = config2.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()

        assertEquals(generalConfig2, actualGeneralConfig)
        assertEquals(warnConfig1, actualWarnConfig)
    }

    @Test
    fun `merge two configs with different fields`() {
        createTomlFiles()
        val config1 = TestConfig(toml1, null, mutableListOf(generalConfig1, warnConfig2, fixConfig1), fs)
        val config2 = TestConfig(toml2, config1, mutableListOf(generalConfig2, warnConfig3, fixConfig2), fs)

        config2.mergeConfigWithParents()

        assertEquals(3, config2.pluginConfigs.size)

        val expectedGeneralConfig =
                GeneralConfig("", listOf("Tag11", "Tag12", "Tag21"), "Description2", "suiteName2", listOf("excludedTests: test3"), listOf("includedTests: test4"))
        val expectedWarnConfig = WarnPluginConfig("execCmd3", warningsOutputPattern2, extraFlagsPattern2,
            true, false, 1, ", ", 3, 3, 3, 3, 3, 3, 3, true)
        val expectedFixConfig = FixPluginConfig("fixCmd2", 1, "Suffix")

        val actualGeneralConfig = config2.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        val actualWarnConfig = config2.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()
        val actualFixConfig = config2.pluginConfigs.filterIsInstance<FixPluginConfig>().first()

        assertEquals(expectedGeneralConfig, actualGeneralConfig)
        assertEquals(expectedWarnConfig, actualWarnConfig)
        assertEquals(expectedFixConfig, actualFixConfig)
    }

    @Test
    fun `merge configs with many parents`() {
        createTomlFiles()
        val config1 = TestConfig(toml1, null, mutableListOf(generalConfig1, warnConfig1, fixConfig1), fs)
        val config2 = TestConfig(toml2, config1, mutableListOf(generalConfig2, warnConfig2, fixConfig2), fs)
        val config3 = TestConfig(toml3, config2, mutableListOf(generalConfig3, warnConfig3, fixConfig3), fs)
        val config4 = TestConfig(toml4, config3, mutableListOf(generalConfig4, warnConfig4, fixConfig4), fs)

        config4.mergeConfigWithParents()

        assertEquals(3, config4.pluginConfigs.size)
        val expectedGeneralConfig =
                GeneralConfig("", listOf("Tag11", "Tag12", "Tag21", "Tag31", "Tag32"), "Description2", "suiteName4", listOf("excludedTests: test7"), listOf("includedTests: test8"))
        val expectedWarnConfig = WarnPluginConfig("execCmd4", warningsOutputPattern2, extraFlagsPattern2,
            true, false, 1, ", ", 4, 4, 4, 4, 4, 4, 4, true)
        val expectedFixConfig = FixPluginConfig("fixCmd4", 1, "Suffix")

        val actualGeneralConfig = config4.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        val actualWarnConfig = config4.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()
        val actualFixConfig = config4.pluginConfigs.filterIsInstance<FixPluginConfig>().first()

        assertEquals(expectedGeneralConfig, actualGeneralConfig)
        assertEquals(expectedWarnConfig, actualWarnConfig)
        assertEquals(expectedFixConfig, actualFixConfig)
    }

    @Test
    fun `merge real toml configs with empty execFlag in child`() {
        // stub, since tearDown should delete it anyway
        createTomlFiles()

        val toml1 = "src/commonNonJsTest/resources/merge_configs/save.toml"
        val configList1 = createPluginConfigListFromToml(toml1.toPath(), fs)

        val parentGeneralConfig = configList1.filterIsInstance<GeneralConfig>().first()
        val parentWarnConfig = configList1.filterIsInstance<WarnPluginConfig>().first()
        assertEquals("echo hello world", parentGeneralConfig.execCmd)
        assertEquals(listOf("Tag"), parentGeneralConfig.tags)
        assertEquals(null, parentWarnConfig.execFlags)

        val toml2 = "src/commonNonJsTest/resources/merge_configs/inner/save.toml"
        val configList2 = createPluginConfigListFromToml(toml2.toPath(), fs)

        val childGeneralConfig = configList2.filterIsInstance<GeneralConfig>().first()
        val childWarnConfig = configList2.filterIsInstance<WarnPluginConfig>().first()

        assertEquals(listOf(""), childGeneralConfig.tags)
        assertEquals(null, childWarnConfig.execFlags)

        val testConfig1 = TestConfig(toml1.toPath(), null, configList1.toMutableList(), fs)
        val testConfig2 = TestConfig(toml2.toPath(), testConfig1, configList2.toMutableList(), fs)

        val mergedTestConfig = testConfig2.mergeConfigWithParents()
        testConfig2.validateAndSetDefaults()

        val mergedGeneralConfig = mergedTestConfig.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        val mergedWarnConfig = mergedTestConfig.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()

        assertEquals(listOf("Tag", ""), mergedGeneralConfig.tags)
        // execFlags should be empty, not `"null"`
        assertEquals("", mergedWarnConfig.execFlags)
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }
}

internal fun createTomlFiles() {
    fs.createDirectory(tmpDir)
    fs.createFile(toml1)
    fs.createDirectory(nestedDir1)
    fs.createFile(toml2)
    fs.createDirectory(nestedDir2)
    fs.createFile(toml3)
    fs.createDirectory(nestedDir2 / "nestedDir3")
    fs.createDirectory(nestedDir2 / "nestedDir3" / "nestedDir4")
    fs.createFile(toml4)
}
