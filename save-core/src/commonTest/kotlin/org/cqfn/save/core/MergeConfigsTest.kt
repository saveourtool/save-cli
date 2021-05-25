package org.cqfn.save.core

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.ConfigDetector
import org.cqfn.save.core.files.MergeConfigs
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.plugin.warn.WarnPluginConfig
import org.cqfn.save.plugins.fix.FixPluginConfig

import okio.FileSystem

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("TOO_LONG_FUNCTION", "LOCAL_VARIABLE_EARLY_DECLARATION")
class MergeConfigsTest {
    private val fs = FileSystem.SYSTEM
    private val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / MergeConfigsTest::class.simpleName!!).also {
        fs.createDirectory(it)
    }

    private val mergeConfigs = MergeConfigs()

    @Test
    fun `merge two incomplete configs`() {
        val toml1 = fs.createFile(tmpDir / "save.toml")

        val nestedDir1 = tmpDir / "nestedDir1"
        fs.createDirectory(nestedDir1)
        val toml2 = fs.createFile(nestedDir1 / "save.toml")

        val generalConfig1 = GeneralConfig("Tag1", "Description1", "excludedTests: test1", "includedTests: test2")
        val generalConfig2 = GeneralConfig("Tag2", "Description2", "excludedTests: test3", "includedTests: test4")

        val warningsInputPattern = Regex("// ;warn:(\\d+):(\\d+): (.*)")
        val warningsOutputPattern = Regex("\\w+ - (\\d+)/(\\d+) - (.*)$")

        val warnConfig1 = WarnPluginConfig("execCmd1", warningsInputPattern, warningsOutputPattern,
            false, false, 1, 1, 1)

        val config1 = TestConfig(toml1, null, mutableListOf(generalConfig1, warnConfig1))
        val config2 = TestConfig(toml2, config1, mutableListOf(generalConfig2))

        mergeConfigs.merge(config2)

        assertEquals(2, config2.pluginConfigs.size)

        val actualGeneralConfig = config2.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        val actualWarnConfig = config2.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()

        assertEquals(generalConfig2, actualGeneralConfig)
        assertEquals(warnConfig1, actualWarnConfig)
    }

    @Test
    fun `merge two configs with different fields`() {
        val toml1 = fs.createFile(tmpDir / "save.toml")

        val nestedDir1 = tmpDir / "nestedDir1"
        fs.createDirectory(nestedDir1)
        val toml2 = fs.createFile(nestedDir1 / "save.toml")

        val generalConfig1 = GeneralConfig("Tag1", "Description1", "excludedTests: test1", "includedTests: test2")
        val generalConfig2 = GeneralConfig("Tag2", "Description2", "excludedTests: test3", "includedTests: test4")

        val warnConfig1 = WarnPluginConfig("execCmd1", Regex(".*"), Regex(".*"),
            false, false, 1, 1, 1)

        val warningsInputPattern = Regex("// ;warn:(\\d+):(\\d+): (.*)")
        val warningsOutputPattern = Regex("\\w+ - (\\d+)/(\\d+) - (.*)$")

        val warnConfig2 = WarnPluginConfig("execCmd2", warningsInputPattern, warningsOutputPattern,
            warningTextHasColumn = true, lineCaptureGroup = 2, columnCaptureGroup = 2, messageCaptureGroup = 2)
        val expectedWarnConfig = WarnPluginConfig("execCmd2", warningsInputPattern, warningsOutputPattern,
            false, true, 2, 2, 2)

        val fixConfig1 = FixPluginConfig("fixCmd1", false, "some suffix")
        val fixConfig2 = FixPluginConfig("fixCmd2", true, null)
        val expectedFixConfig = FixPluginConfig("fixCmd2", true, "some suffix")

        val config1 = TestConfig(toml1, null, mutableListOf(generalConfig1, warnConfig1, fixConfig1))
        val config2 = TestConfig(toml2, config1, mutableListOf(generalConfig2, warnConfig2, fixConfig2))

        mergeConfigs.merge(config2)

        assertEquals(3, config2.pluginConfigs.size)

        val actualGeneralConfig = config2.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        val actualWarnConfig = config2.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()
        val actualFixConfig = config2.pluginConfigs.filterIsInstance<FixPluginConfig>().first()

        assertEquals(generalConfig2, actualGeneralConfig)
        assertEquals(expectedWarnConfig, actualWarnConfig)
        assertEquals(expectedFixConfig, actualFixConfig)
    }

    @Test
    fun `merge many configs`() {
        val toml1 = fs.createFile(tmpDir / "save.toml")

        val nestedDir1 = tmpDir / "nestedDir1"
        fs.createDirectory(nestedDir1)
        val toml2 = fs.createFile(nestedDir1 / "save.toml")

        val nestedDir2 = tmpDir / "nestedDir1" / "nestedDir2"
        fs.createDirectory(nestedDir2)
        val toml3 = fs.createFile(nestedDir2 / "save.toml")

        fs.createDirectory(nestedDir2 / "nestedDir3")
        fs.createDirectory(nestedDir2 / "nestedDir3" / "nestedDir4")
        val toml4 = fs.createFile(nestedDir2 / "nestedDir3" / "nestedDir4" / "save.toml")

        val generalConfig1 = GeneralConfig("Tag1", "Description1", "excludedTests: test1", "includedTests: test2")
        val generalConfig2 = GeneralConfig("Tag2", "Description2", "excludedTests: test3", "includedTests: test4")
        val generalConfig3 = GeneralConfig("Tag3", "Description2", "excludedTests: test5", "includedTests: test6")
        val generalConfig4 = GeneralConfig("Tag4", "Description2", "excludedTests: test7", "includedTests: test8")

        val warnConfig1 = WarnPluginConfig("execCmd1", Regex(".*"), Regex(".*"),
            true, true, 1, 1, 1)
        val warnConfig2 = WarnPluginConfig("execCmd2", Regex(".* \\w"), Regex(".* \\w"),
            warningTextHasColumn = false, lineCaptureGroup = 2, columnCaptureGroup = 2, messageCaptureGroup = 2)

        val warnConfig3 = WarnPluginConfig("execCmd3", Regex(".* foo \\w"), Regex(".* foo \\w"),
            warningTextHasColumn = true, lineCaptureGroup = 3, columnCaptureGroup = 3, messageCaptureGroup = 3)

        val warningsInputPattern = Regex("// ;warn:(\\d+):(\\d+): (.*)")
        val warningsOutputPattern = Regex("\\w+ - (\\d+)/(\\d+) - (.*)$")

        val warnConfig4 = WarnPluginConfig("execCmd4", warningsInputPattern, warningsOutputPattern,
            lineCaptureGroup = 4, columnCaptureGroup = 4, messageCaptureGroup = 4)

        val expectedWarnConfig = WarnPluginConfig("execCmd4", warningsInputPattern, warningsOutputPattern,
            true, true, 4, 4, 4)

        val fixConfig1 = FixPluginConfig("fixCmd1", destinationFileSuffix = "some suffix")
        val fixConfig2 = FixPluginConfig("fixCmd2", true)
        val fixConfig3 = FixPluginConfig("fixCmd3", null, null)
        val fixConfig4 = FixPluginConfig("fixCmd4")
        val expectedFixConfig = FixPluginConfig("fixCmd4", true, "some suffix")

        val config1 = TestConfig(toml1, null, mutableListOf(generalConfig1, warnConfig1, fixConfig1))
        val config2 = TestConfig(toml2, config1, mutableListOf(generalConfig2, warnConfig2, fixConfig2))
        val config3 = TestConfig(toml3, config2, mutableListOf(generalConfig3, warnConfig3, fixConfig3))
        val config4 = TestConfig(toml4, config3, mutableListOf(generalConfig4, warnConfig4, fixConfig4))

        mergeConfigs.merge(config4)

        assertEquals(3, config4.pluginConfigs.size)

        val actualGeneralConfig = config4.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        val actualWarnConfig = config4.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()
        val actualFixConfig = config4.pluginConfigs.filterIsInstance<FixPluginConfig>().first()

        assertEquals(generalConfig4, actualGeneralConfig)
        assertEquals(expectedWarnConfig, actualWarnConfig)
        assertEquals(expectedFixConfig, actualFixConfig)
    }

    @Test
    fun `merge configs starting from the middle`() {
        val toml1 = fs.createFile(tmpDir / "save.toml")
        val nestedDir1 = tmpDir / "nestedDir1"
        fs.createDirectory(nestedDir1)
        val toml2 = fs.createFile(nestedDir1 / "save.toml")

        val nestedDir2 = tmpDir / "nestedDir1" / "nestedDir2"
        fs.createDirectory(nestedDir2)
        val toml3 = fs.createFile(nestedDir2 / "save.toml")

        val generalConfig1 = GeneralConfig("Tag1", "Description1", "excludedTests: test1", "includedTests: test2")
        val generalConfig2 = GeneralConfig("Tag2", "Description2", "excludedTests: test3", "includedTests: test4")
        val generalConfig3 = GeneralConfig("Tag3", "Description2", "excludedTests: test5", "includedTests: test6")

        val warningsInputPattern = Regex("// ;warn:(\\d+):(\\d+): (.*)")
        val warningsOutputPattern = Regex("\\w+ - (\\d+)/(\\d+) - (.*)$")

        val warnConfig1 = WarnPluginConfig("execCmd1", Regex(".*"), Regex(".*"),
            true, true, 1, 1, 1)
        val warnConfig2 = WarnPluginConfig("execCmd2", warningsInputPattern, warningsOutputPattern,
            warningTextHasColumn = false, lineCaptureGroup = 2, columnCaptureGroup = 2, messageCaptureGroup = 2)


        val fixConfig1 = FixPluginConfig("fixCmd1", destinationFileSuffix = "some suffix")
        val fixConfig2 = FixPluginConfig("fixCmd2", true)

        val config1 = TestConfig(toml1, null, mutableListOf(generalConfig1, warnConfig1, fixConfig1))
        val config2 = TestConfig(toml2, config1, mutableListOf(generalConfig2, warnConfig2, fixConfig2))
        val config3 = TestConfig(toml3, config2, mutableListOf(generalConfig3))

        mergeConfigs.merge(config2)

        val expectedWarnConfig2 = WarnPluginConfig("execCmd2", warningsInputPattern, warningsOutputPattern,
            true, false, 2, 2, 2)
        val expectedFixConfig2 = FixPluginConfig("fixCmd2", true, "some suffix")

        assertEquals(3, config2.pluginConfigs.size)

        val actualGeneralConfig2 = config2.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        val actualWarnConfig2 = config2.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()
        val actualFixConfig2 = config2.pluginConfigs.filterIsInstance<FixPluginConfig>().first()

        assertEquals(generalConfig2, actualGeneralConfig2)
        assertEquals(expectedWarnConfig2, actualWarnConfig2)
        assertEquals(expectedFixConfig2, actualFixConfig2)

        val actualGeneralConfig3 = config3.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        val actualWarnConfig3 = config3.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()
        val actualFixConfig3 = config3.pluginConfigs.filterIsInstance<FixPluginConfig>().first()

        assertEquals(generalConfig3, actualGeneralConfig3)
        assertEquals(expectedWarnConfig2, actualWarnConfig3)
        assertEquals(expectedFixConfig2, actualFixConfig3)
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }
}
