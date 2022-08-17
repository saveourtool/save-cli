package com.saveourtool.save.core

import com.saveourtool.save.core.config.EvaluatedToolConfig
import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.plugin.warn.WarnPluginConfig
import com.saveourtool.save.plugins.fix.FixPluginConfig

import okio.FileSystem

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("LONG_LINE")
class ValidationTest {
    private val fs: FileSystem = FileSystem.SYSTEM

    private val emptyEvaluatedToolConfig = EvaluatedToolConfig(
        null, null, 1, ", "
    )

    @Test
    fun `set defaults to general section`() {
        createTomlFiles()
        val generalConfig = GeneralConfig("exeCmd", tags = listOf("Tag11", "Tag12"), description = "Description1", suiteName = "suiteName1")
        val config = TestConfig(toml1, null, mutableListOf(generalConfig), fs)

        config.validateAndSetDefaults(emptyEvaluatedToolConfig)

        assertEquals(1, config.pluginConfigs.size)

        val actualGeneralConfig1 = config.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        assertEquals(emptyList(), actualGeneralConfig1.excludedTests)
    }

    @Test
    fun `invalid general section`() {
        createTomlFiles()
        val generalConfig = GeneralConfig()
        val config = TestConfig(toml1, null, mutableListOf(generalConfig), fs)

        generalConfig.configLocation = config.location
        try {
            config.validateAndSetDefaults(emptyEvaluatedToolConfig)
        } catch (ex: IllegalArgumentException) {
            assertEquals(
                """
                    Error: Couldn't find `execCmd` in [general] section of `${generalConfig.configLocation}` config.
                    Current configuration: ${generalConfig.toString().substringAfter("(").substringBefore(")")}
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
        val config = TestConfig(toml1, null, mutableListOf(warnConfig), fs)

        config.validateAndSetDefaults(emptyEvaluatedToolConfig)

        assertEquals(1, config.pluginConfigs.size)

        val actualWarnConfig = config.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()
        assertEquals(Regex("(.+):(\\d*):(\\d*): (.+)").toString(), actualWarnConfig.actualWarningsPattern.toString())
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
        assertEquals(".*Test.*", actualWarnConfig.testNameRegex)
    }

    // Add proper values for `lineCaptureGroup` and `columnCaptureGroup` according
    // `warningTextHasLine` `warningTextHasColumn`
    @Test
    fun `validate warn section`() {
        createTomlFiles()
        val warnConfig = WarnPluginConfig(execFlags = "execFlags", warningTextHasLine = true, warningTextHasColumn = false)
        val config = TestConfig(toml1, null, mutableListOf(warnConfig), fs)

        config.validateAndSetDefaults(emptyEvaluatedToolConfig)

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
        val config = TestConfig(toml1, null, mutableListOf(warnConfig), fs)

        config.validateAndSetDefaults(emptyEvaluatedToolConfig)

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
        val config = TestConfig(toml1, null, mutableListOf(warnConfig), fs)

        config.validateAndSetDefaults(emptyEvaluatedToolConfig)

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
        val config = TestConfig(toml1, null, mutableListOf(warnConfig), fs)
        warnConfig.configLocation = config.location
        try {
            config.validateAndSetDefaults(emptyEvaluatedToolConfig)
        } catch (ex: IllegalArgumentException) {
            assertTrue("Exception message content incorrect: ${ex.message}") {
                ex.message!!.startsWith(
                    "[Configuration Error]: All integer values in [warn] section of `${warnConfig.configLocation}` config should be positive!" +
                            "\nCurrent configuration: "
                )
            }
        }
    }

    @Test
    fun `set defaults to fix section`() {
        createTomlFiles()
        val fixConfig = FixPluginConfig(execFlags = "execFlags")
        val config = TestConfig(toml1, null, mutableListOf(fixConfig), fs)

        config.validateAndSetDefaults(emptyEvaluatedToolConfig)

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
