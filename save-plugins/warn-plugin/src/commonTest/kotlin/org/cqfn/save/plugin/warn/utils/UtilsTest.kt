package org.cqfn.save.plugin.warn.utils

import org.cqfn.save.plugin.warn.WarnPluginConfig
import org.cqfn.save.plugin.warn.WarnPluginConfig.Companion.defaultInputPattern
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Suppress("MAGIC_NUMBER")
class UtilsTest {
    @Test
    fun `should extract warnings from different text with line and col`() {
        val config = WarnPluginConfig("stub", defaultInputPattern, Regex("stub"), warningTextHasLine = true, warningTextHasColumn = true,
            lineCaptureGroup = 1, columnCaptureGroup = 2, messageCaptureGroup = 3)
        assertExtracts(config, ";warn:1:2: Foo bar baz", Warning("Foo bar baz", 1, 2, "Test.kt"))
        assertExtracts(config, ";warn:1:2:  Foo bar baz", Warning(" Foo bar baz", 1, 2, "Test.kt"))
        assertExtractionFails(config, "warn:1:2 Foo bar baz")
        assertExtractionFails(config, ";warn:1: Foo bar baz")
        assertExtractionFails(config, ";warn:1:: Foo bar baz")
        assertExtractionFails(config, ";warn:1:2:")
        assertExtractionFails(config, ";warn:1:2: ")
    }

    @Test
    fun `should extract warnings from different text with no line but col`() {
        val config = WarnPluginConfig("stub", Regex(";warn:(\\d+): (.+)"), Regex("stub"), warningTextHasLine = false, warningTextHasColumn = true,
            lineCaptureGroup = null, columnCaptureGroup = 1, messageCaptureGroup = 2)
        assertExtracts(config, ";warn:2: Foo bar baz", Warning("Foo bar baz", null, 2, "Test.kt"))
        assertExtracts(config, ";warn:2:  Foo bar baz", Warning(" Foo bar baz", null, 2, "Test.kt"))
        assertExtractionFails(config, "warn:1:2 Foo bar baz")
        assertExtractionFails(config, ";warn::1 Foo bar baz")
        assertExtractionFails(config, ";warn:1:: Foo bar baz")
        assertExtractionFails(config, ";warn:1:")
        assertExtractionFails(config, ";warn:1: ")
    }

    @Test
    fun `should extract warnings from different text with line but no col`() {
        val config = WarnPluginConfig("stub", Regex(";warn:(\\d+): (.+)"), Regex("stub"), warningTextHasLine = true, warningTextHasColumn = false,
            lineCaptureGroup = 1, columnCaptureGroup = null, messageCaptureGroup = 2)
        assertExtracts(config, ";warn:2: Foo bar baz", Warning("Foo bar baz", 2, null, "Test.kt"))
        assertExtracts(config, ";warn:2:  Foo bar baz", Warning(" Foo bar baz", 2, null, "Test.kt"))
        assertExtractionFails(config, "warn:1:2 Foo bar baz")
        assertExtractionFails(config, ";warn::1 Foo bar baz")
        assertExtractionFails(config, ";warn:1:: Foo bar baz")
        assertExtractionFails(config, ";warn:1:")
        assertExtractionFails(config, ";warn:1: ")
    }

    private fun assertExtracts(
        warnPluginConfig: WarnPluginConfig,
        text: String,
        expectedWarning: Warning) {
        val warning = text.extractWarning(
            false,
            warnPluginConfig.warningsInputPattern!!,
            fileName = "Test.kt",
            lineGroupIdx = warnPluginConfig.lineCaptureGroup,
            columnGroupIdx = warnPluginConfig.columnCaptureGroup,
            messageGroupIdx = warnPluginConfig.messageCaptureGroup!!
        )
        requireNotNull(warning)
        assertEquals(expectedWarning, warning)
    }

    private fun assertExtractionFails(warnPluginConfig: WarnPluginConfig, text: String) {
        val warning = text.extractWarning(
            false,
            warnPluginConfig.warningsInputPattern!!,
            fileName = "fileName",
            lineGroupIdx = warnPluginConfig.lineCaptureGroup,
            columnGroupIdx = warnPluginConfig.columnCaptureGroup,
            messageGroupIdx = warnPluginConfig.messageCaptureGroup!!
        )
        assertNull(warning)
    }
}
