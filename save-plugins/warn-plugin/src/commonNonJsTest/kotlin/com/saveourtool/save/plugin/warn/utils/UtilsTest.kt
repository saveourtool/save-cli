package com.saveourtool.save.plugin.warn.utils

import com.saveourtool.save.core.plugin.GeneralConfig.Companion.defaultExpectedWarningPattern
import com.saveourtool.save.plugin.warn.WarnPluginConfig

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Suppress("MAGIC_NUMBER")
class UtilsTest {
    @Test
    fun `should extract warnings from different text with line and col`() {
        val config = WarnPluginConfig(
            "stub", Regex("stub"), warningTextHasLine = true, warningTextHasColumn = true,
            lineCaptureGroup = 1, columnCaptureGroup = 2, messageCaptureGroup = 3, linePlaceholder = "line"
        )
        assertExtracts(
            config,
            "// ;warn:1:2: Foo bar baz",
            Warning("Foo bar baz", 1, 2, "Test.kt"),
            defaultExpectedWarningPattern
        )
        assertExtracts(
            config,
            "// ;warn:1:2:  Foo bar baz",
            Warning("Foo bar baz", 1, 2, "Test.kt"),
            defaultExpectedWarningPattern
        )
        assertExtractionFails(config, "// warn:1:2 Foo bar baz", defaultExpectedWarningPattern)
        assertExtractionFails(config, "// ;warn:1: Foo bar baz", defaultExpectedWarningPattern)
        assertExtractionFails(config, "// ;warn:1:: Foo bar baz", defaultExpectedWarningPattern)
        assertExtractionFails(config, "// ;warn:1:2:", defaultExpectedWarningPattern)
        assertExtractionFails(config, "// ;warn:1:2: ", defaultExpectedWarningPattern)
    }

    @Test
    fun `should extract warnings from different text with no line but col`() {
        val config = WarnPluginConfig(
            "stub", Regex("stub"), warningTextHasLine = false, warningTextHasColumn = true,
            lineCaptureGroup = null, columnCaptureGroup = 1, messageCaptureGroup = 2, linePlaceholder = "line"
        )
        assertExtracts(
            config,
            ";warn:2: Foo bar baz",
            Warning("Foo bar baz", null, 2, "Test.kt"),
            Regex(";warn:(\\d+): (.+)")
        )
        assertExtracts(
            config,
            ";warn:2:  Foo bar baz",
            Warning("Foo bar baz", null, 2, "Test.kt"),
            Regex(";warn:(\\d+): (.+)")
        )
        assertExtractionFails(config, "warn:1:2 Foo bar baz", Regex(";warn:(\\d+): (.+)"))
        assertExtractionFails(config, ";warn::1 Foo bar baz", Regex(";warn:(\\d+): (.+)"))
        assertExtractionFails(config, ";warn:1:: Foo bar baz", Regex(";warn:(\\d+): (.+)"))
        assertExtractionFails(config, ";warn:1:", Regex(";warn:(\\d+): (.+)"))
        assertExtractionFails(config, ";warn:1: ", Regex(";warn:(\\d+): (.+)"))
    }

    @Test
    fun `should extract warnings from different text with line but no col`() {
        val config = WarnPluginConfig(
            "stub", Regex("stub"), warningTextHasLine = true, warningTextHasColumn = false,
            lineCaptureGroup = 1, columnCaptureGroup = null, messageCaptureGroup = 2, linePlaceholder = "line"
        )
        assertExtracts(
            config,
            ";warn:2: Foo bar baz",
            Warning("Foo bar baz", 2, null, "Test.kt"),
            Regex(";warn:(\\d+): (.+)")
        )
        assertExtracts(
            config,
            ";warn:2:  Foo bar baz",
            Warning("Foo bar baz", 2, null, "Test.kt"),
            Regex(";warn:(\\d+): (.+)")
        )
        assertExtractionFails(config, "warn:1:2 Foo bar baz", Regex(";warn:(\\d+): (.+)"))
        assertExtractionFails(config, ";warn::1 Foo bar baz", Regex(";warn:(\\d+): (.+)"))
        assertExtractionFails(config, ";warn:1:: Foo bar baz", Regex(";warn:(\\d+): (.+)"))
        assertExtractionFails(config, ";warn:1:", Regex(";warn:(\\d+): (.+)"))
        assertExtractionFails(config, ";warn:1: ", Regex(";warn:(\\d+): (.+)"))
    }

    private fun assertExtracts(
        warnPluginConfig: WarnPluginConfig,
        text: String,
        expectedWarning: Warning,
        expectedWarningsPattern: Regex
    ) {
        val line = text.getLineNumber(
            expectedWarningsPattern,
            warnPluginConfig.lineCaptureGroup,
            warnPluginConfig.linePlaceholder!!,
            null,
            null,
            null,
        )

        val warning = text.extractWarning(
            expectedWarningsPattern,
            fileName = "Test.kt",
            line = line,
            columnGroupIdx = warnPluginConfig.columnCaptureGroup,
            messageGroupIdx = warnPluginConfig.messageCaptureGroup!!,
            benchmarkMode = false
        )

        requireNotNull(warning)
        assertEquals(expectedWarning, warning)
    }

    private fun assertExtractionFails(
        warnPluginConfig: WarnPluginConfig,
        text: String,
        expectedWarningsPattern: Regex
    ) {
        val line = text.getLineNumber(
            expectedWarningsPattern,
            warnPluginConfig.lineCaptureGroup,
            warnPluginConfig.linePlaceholder!!,
            null,
            null,
            null,
        )
        val warning = text.extractWarning(
            expectedWarningsPattern,
            fileName = "fileName",
            line = line,
            columnGroupIdx = warnPluginConfig.columnCaptureGroup,
            messageGroupIdx = warnPluginConfig.messageCaptureGroup!!,
            benchmarkMode = false
        )
        assertNull(warning)
    }
}
