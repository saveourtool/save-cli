package org.cqfn.save.core

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.plugin.warn.WarnPluginConfig
import org.cqfn.save.plugins.fix.FixPluginConfig
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidationTest {
    @Test
    fun `set defaults to general section`() {
        createTomlFiles()
        val generalConfig = GeneralConfig("exeCmd", tags = "Tag11, Tag12", description = "Description1", suiteName = "suiteName1")
        val config = TestConfig(toml1, null, mutableListOf(generalConfig))

        config.validateAndSetDefaults()

        assertEquals(1, config.pluginConfigs.size)

        val actualGeneralConfig1 = config.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        assertEquals("", actualGeneralConfig1.excludedTests)
        assertEquals("", actualGeneralConfig1.includedTests)
        assertEquals(false, actualGeneralConfig1.ignoreSaveComments)
    }

    @Test
    fun `invalid general section`() {
        createTomlFiles()
        val generalConfig = GeneralConfig()
        val config = TestConfig(toml1, null, mutableListOf(generalConfig))
        generalConfig.configLocation = config.location
        try {
            config.validateAndSetDefaults()
        } catch (ex: IllegalArgumentException) {
            assertEquals(
                """
                    Error: Couldn't find `execCmd` in [general] section of `${generalConfig.configLocation}` config.
                    Current configuration: execCmd=null, tags=null, description=null, suiteName=null, excludedTests=null, includedTests=null, ignoreSaveComments=null
                    Please provide it in this, or at least in one of the parent configs.
                """.trimIndent(),
                ex.message
            )
        }
    }

    @Test
    fun `set defaults to warn section`() {
        createTomlFiles()
        val warnConfig = WarnPluginConfig(execFlags = "execFlags", messageCaptureGroup = 4)
        val config = TestConfig(toml1, null, mutableListOf(warnConfig))

        config.validateAndSetDefaults()

        assertEquals(1, config.pluginConfigs.size)

        val actualWarnConfig = config.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()
        assertEquals(Regex(";warn:(\\d+):(\\d+): (.+)").toString(), actualWarnConfig.warningsInputPattern.toString())
        assertEquals(Regex("(.+):(\\d+):(\\d+): (.+)").toString(), actualWarnConfig.warningsOutputPattern.toString())
        assertEquals(true, actualWarnConfig.warningTextHasLine)
        assertEquals(true, actualWarnConfig.warningTextHasColumn)
        assertEquals(1, actualWarnConfig.lineCaptureGroup)
        assertEquals(2, actualWarnConfig.columnCaptureGroup)
        assertEquals(4, actualWarnConfig.messageCaptureGroup)
        assertEquals(1, actualWarnConfig.fileNameCaptureGroupOut)
        assertEquals(2, actualWarnConfig.lineCaptureGroupOut)
        assertEquals(3, actualWarnConfig.columnCaptureGroupOut)
        assertEquals(4, actualWarnConfig.messageCaptureGroupOut)
        assertEquals(true, actualWarnConfig.exactWarningsMatch)
        assertEquals("Test", actualWarnConfig.testNameSuffix)
    }

    // Add proper values for `lineCaptureGroup` and `columnCaptureGroup` according
    // `warningTextHasLine` `warningTextHasColumn`
    @Test
    fun `validate warn section`() {
        createTomlFiles()
        val warnConfig = WarnPluginConfig(execFlags = "execFlags", warningTextHasLine = true, warningTextHasColumn = false)
        val config = TestConfig(toml1, null, mutableListOf(warnConfig))

        config.validateAndSetDefaults()

        assertEquals(1, config.pluginConfigs.size)

        val actualWarnConfig = config.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()
        assertEquals(true, actualWarnConfig.warningTextHasLine)
        assertEquals(false, actualWarnConfig.warningTextHasColumn)
        assertEquals(1, actualWarnConfig.lineCaptureGroup)
        assertEquals(null, actualWarnConfig.columnCaptureGroup)
    }

    // Provided incorrect values `warningTextHasLine = false` but `lineCaptureGroup = 2`; validate it
    @Test
    fun `validate warn section 2`() {
        createTomlFiles()
        val warnConfig = WarnPluginConfig(execFlags = "execFlags", warningTextHasLine = false, lineCaptureGroup = 1)
        val config = TestConfig(toml1, null, mutableListOf(warnConfig))

        config.validateAndSetDefaults()

        assertEquals(1, config.pluginConfigs.size)

        val actualWarnConfig = config.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()
        assertEquals(false, actualWarnConfig.warningTextHasLine)
        assertEquals(true, actualWarnConfig.warningTextHasColumn)
        assertEquals(null, actualWarnConfig.lineCaptureGroup)
        assertEquals(2, actualWarnConfig.columnCaptureGroup)
    }

    // `lineCaptureGroup` provided, but `warningTextHasLine` is absent; validate it
    @Test
    fun `validate warn section 3`() {
        createTomlFiles()
        val warnConfig = WarnPluginConfig(execFlags = "execFlags", lineCaptureGroup = 5)
        val config = TestConfig(toml1, null, mutableListOf(warnConfig))

        config.validateAndSetDefaults()

        assertEquals(1, config.pluginConfigs.size)

        val actualWarnConfig = config.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()
        assertEquals(true, actualWarnConfig.warningTextHasLine)
        assertEquals(true, actualWarnConfig.warningTextHasColumn)
        assertEquals(5, actualWarnConfig.lineCaptureGroup)
        assertEquals(2, actualWarnConfig.columnCaptureGroup)
    }

    // `lineCaptureGroup` provided, but incorrect -- error
    @Test
    fun `validate warn section 4`() {
        createTomlFiles()
        val warnConfig = WarnPluginConfig(execFlags = "execFlags", lineCaptureGroup = -127)
        val config = TestConfig(toml1, null, mutableListOf(warnConfig))
        warnConfig.configLocation = config.location
        try {
            config.validateAndSetDefaults()
        } catch (ex: IllegalArgumentException) {
            assertEquals(
                "Error: All integer values in [warn] section of `${warnConfig.configLocation}` config should be positive!" +
                        "\nCurrent configuration: execFlags=execFlags, warningsInputPattern=null, warningsOutputPattern=null, " +
                        "warningTextHasLine=null, warningTextHasColumn=null, batchSize=null, lineCaptureGroup=-127, columnCaptureGroup=null, " +
                        "messageCaptureGroup=null, fileNameCaptureGroupOut=null, lineCaptureGroupOut=null, columnCaptureGroupOut=null, messageCaptureGroupOut=null, " +
                        "exactWarningsMatch=null, testNameSuffix=null",
                ex.message
            )
        }
    }

    @Test
    fun `set defaults to fix section`() {
        createTomlFiles()
        val fixConfig = FixPluginConfig(execFlags = "execFlags")
        val config = TestConfig(toml1, null, mutableListOf(fixConfig))

        config.validateAndSetDefaults()

        assertEquals(1, config.pluginConfigs.size)

        val actualFixConfig = config.pluginConfigs.filterIsInstance<FixPluginConfig>().first()
        assertEquals("Test", actualFixConfig.resourceNameTest)
        assertEquals("Expected", actualFixConfig.resourceNameExpected)
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }
}
