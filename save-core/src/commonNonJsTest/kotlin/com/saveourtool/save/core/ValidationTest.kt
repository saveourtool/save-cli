package com.saveourtool.save.core

import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.plugin.PluginConfig
import com.saveourtool.save.core.utils.singleIsInstance
import com.saveourtool.save.core.utils.validateAndSetDefaults
import com.saveourtool.save.plugin.warn.WarnPluginConfig
import com.saveourtool.save.plugins.fix.FixPluginConfig

import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ValidationTest {
    @Test
    fun `set defaults to general section`() {
        val generalConfig = GeneralConfig("exeCmd", tags = listOf("Tag11", "Tag12"), description = "Description1", suiteName = "suiteName1")
        val config: MutableList<PluginConfig> = mutableListOf(generalConfig)

        config.validateAndSetDefaults()

        assertEquals(1, config.size)

        val actualGeneralConfig1: GeneralConfig = config.singleIsInstance()
        assertEquals(emptyList(), actualGeneralConfig1.excludedTests)
        assertEquals(1, actualGeneralConfig1.batchSize)
        assertEquals(", ", actualGeneralConfig1.batchSeparator)
    }

    @Test
    fun `invalid general section`() {
        val generalConfig = GeneralConfig().apply {
            configLocation = "./some-path".toPath()
        }

        assertException(generalConfig) { ex ->
            assertEquals(
                """
                    Error: Couldn't find `execCmd` in [general] section of `${generalConfig.configLocation}` config.
                    Current configuration: ${generalConfig.toString().substringAfter("(").substringBefore(")")}
                    Please provide it in this, or at least in one of the parent configs.
                """.trimIndent(),
                ex.message
            )
        }

        val generalConfigWithInvalidBatchSize = generalConfig.copy(execCmd = "execCmd", batchSize = -1)
        assertException(generalConfigWithInvalidBatchSize) { ex ->
            assertEquals(
                """
                    [Configuration Error]: `batchSize` in [general] section of `${generalConfigWithInvalidBatchSize.configLocation}` config should be positive!
                    Current configuration: ${generalConfigWithInvalidBatchSize.toString().substringAfter("(").substringBefore(")")}
                """.trimIndent(),
                ex.message
            )
        }
    }

    @Test
    fun `set defaults to warn section`() {
        val warnConfig = WarnPluginConfig(execFlags = "execFlags", messageCaptureGroup = 4)
        val config: MutableList<PluginConfig> = mutableListOf(warnConfig)

        config.validateAndSetDefaults()

        assertEquals(1, config.size)

        val actualWarnConfig: WarnPluginConfig = config.singleIsInstance()
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
        val warnConfig = WarnPluginConfig(execFlags = "execFlags", warningTextHasLine = true, warningTextHasColumn = false)
        val config: MutableList<PluginConfig> = mutableListOf(warnConfig)

        config.validateAndSetDefaults()

        assertEquals(1, config.size)

        val actualWarnConfig: WarnPluginConfig = config.singleIsInstance()
        assertEquals(true, actualWarnConfig.warningTextHasLine)
        assertEquals(false, actualWarnConfig.warningTextHasColumn)
        assertEquals(1, actualWarnConfig.lineCaptureGroup)
        assertEquals(null, actualWarnConfig.columnCaptureGroup)
    }

    // Provided incorrect values `warningTextHasLine = false` but `lineCaptureGroup = 2`; validate it
    @Test
    fun `validate warn section 2`() {
        val warnConfig = WarnPluginConfig(execFlags = "execFlags", warningTextHasLine = false, lineCaptureGroup = 1)
        val config: MutableList<PluginConfig> = mutableListOf(warnConfig)

        config.validateAndSetDefaults()

        assertEquals(1, config.size)

        val actualWarnConfig: WarnPluginConfig = config.singleIsInstance()
        assertEquals(false, actualWarnConfig.warningTextHasLine)
        assertEquals(true, actualWarnConfig.warningTextHasColumn)
        assertEquals(null, actualWarnConfig.lineCaptureGroup)
        assertEquals(2, actualWarnConfig.columnCaptureGroup)
    }

    // `lineCaptureGroup` provided, but `warningTextHasLine` is absent; validate it
    @Test
    fun `validate warn section 3`() {
        val warnConfig = WarnPluginConfig(execFlags = "execFlags", lineCaptureGroup = 5)
        val config: MutableList<PluginConfig> = mutableListOf(warnConfig)

        config.validateAndSetDefaults()

        assertEquals(1, config.size)

        val actualWarnConfig: WarnPluginConfig = config.singleIsInstance()
        assertEquals(true, actualWarnConfig.warningTextHasLine)
        assertEquals(true, actualWarnConfig.warningTextHasColumn)
        assertEquals(5, actualWarnConfig.lineCaptureGroup)
        assertEquals(2, actualWarnConfig.columnCaptureGroup)
    }

    // `lineCaptureGroup` provided, but incorrect -- error
    @Test
    fun `validate warn section 4`() {
        val warnConfig = WarnPluginConfig(execFlags = "execFlags", lineCaptureGroup = -127).apply {
            configLocation = "./some-location".toPath()
        }
        assertException(warnConfig) { ex ->
            assertEquals(
                """
                    [Configuration Error]: `lineCaptureGroup` in [warn] section of `${warnConfig.configLocation}` config should be positive!
                    Current configuration: ${warnConfig.toString().substringAfter("(").substringBefore(")")}
                """.trimIndent(),
                ex.message
            )
        }
    }

    @Test
    fun `set defaults to fix section`() {
        val fixConfig = FixPluginConfig(execFlags = "execFlags")
        val config: MutableList<PluginConfig> = mutableListOf(fixConfig)

        config.validateAndSetDefaults()

        assertEquals(1, config.size)

        val actualFixConfig: FixPluginConfig = config.singleIsInstance()
        assertEquals("Test", actualFixConfig.resourceNameTest)
        assertEquals("Expected", actualFixConfig.resourceNameExpected)
    }

    private fun assertException(pluginConfig: PluginConfig, validation: (IllegalArgumentException) -> Unit) {
        try {
            val config: MutableList<PluginConfig> = mutableListOf(pluginConfig)
            config.validateAndSetDefaults()
            fail("Expect exception with type ${IllegalArgumentException::class}")
        } catch (ex: IllegalArgumentException) {
            validation(ex)
        }
    }
}
