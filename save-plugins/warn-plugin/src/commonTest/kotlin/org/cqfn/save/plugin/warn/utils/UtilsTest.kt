package org.cqfn.save.plugin.warn.utils

import org.cqfn.save.plugin.warn.WarnPluginConfig
import org.cqfn.save.plugin.warn.warningRegex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UtilsTest {
    @Test
    fun `should extract warnings from different text with line and col`() {
        val config = WarnPluginConfig("stub", Regex("stub"), warningTextHasLine = true, warningTextHasColumn = true,
            lineCaptureGroup = 1, columnCaptureGroup = 2, messageCaptureGroup = 3)
        assertExtracts(config, ";warn:1:2: Foo bar baz", Warning("Foo bar baz", 1, 2))
        assertExtracts(config, ";warn:1:2:  Foo bar baz", Warning(" Foo bar baz", 1, 2))
        assertExtractionFails(config, "warn:1:2 Foo bar baz")
        assertExtractionFails(config, ";warn:1: Foo bar baz")
        assertExtractionFails(config, ";warn:1:: Foo bar baz")
        assertExtractionFails(config, ";warn:1:2:")
        assertExtractionFails(config, ";warn:1:2: ")
    }

    @Test
    fun `should extract warnings from different text with no line but col`() {
        val config = WarnPluginConfig("stub", Regex("stub"), warningTextHasLine = false, warningTextHasColumn = true,
            lineCaptureGroup = null, columnCaptureGroup = 1, messageCaptureGroup = 2)
        assertExtracts(config, ";warn:2: Foo bar baz", Warning("Foo bar baz", null, 2))
        assertExtracts(config, ";warn:2:  Foo bar baz", Warning(" Foo bar baz", null, 2))
        assertExtractionFails(config, "warn:1:2 Foo bar baz")
        assertExtractionFails(config, ";warn::1 Foo bar baz")
        assertExtractionFails(config, ";warn:1:: Foo bar baz")
        assertExtractionFails(config, ";warn:1:")
        assertExtractionFails(config, ";warn:1: ")
    }

    @Test
    fun `should extract warnings from different text with line but no col`() {
        val config = WarnPluginConfig("stub", Regex("stub"), warningTextHasLine = true, warningTextHasColumn = false,
            lineCaptureGroup = 1, columnCaptureGroup = null, messageCaptureGroup = 2)
        assertExtracts(config, ";warn:2: Foo bar baz", Warning("Foo bar baz", 2, null))
        assertExtracts(config, ";warn:2:  Foo bar baz", Warning(" Foo bar baz", 2, null))
        assertExtractionFails(config, "warn:1:2 Foo bar baz")
        assertExtractionFails(config, ";warn::1 Foo bar baz")
        assertExtractionFails(config, ";warn:1:: Foo bar baz")
        assertExtractionFails(config, ";warn:1:")
        assertExtractionFails(config, ";warn:1: ")
    }

    private fun assertExtracts(warnPluginConfig: WarnPluginConfig, text: String, expectedWarning: Warning) {
        val warning = text.extractWarning(
            warningRegex(warnPluginConfig),
            columnGroupIdx = warnPluginConfig.columnCaptureGroup,
            lineGroupIdx = warnPluginConfig.lineCaptureGroup,
            messageGroupIdx = warnPluginConfig.messageCaptureGroup
        )
        requireNotNull(warning)
        assertEquals(expectedWarning, warning)
    }

    private fun assertExtractionFails(warnPluginConfig: WarnPluginConfig, text: String) {
        val warning = text.extractWarning(
            warningRegex(warnPluginConfig),
            columnGroupIdx = warnPluginConfig.columnCaptureGroup,
            lineGroupIdx = warnPluginConfig.lineCaptureGroup,
            messageGroupIdx = warnPluginConfig.messageCaptureGroup
        )
        assertNull(warning)
    }
}
