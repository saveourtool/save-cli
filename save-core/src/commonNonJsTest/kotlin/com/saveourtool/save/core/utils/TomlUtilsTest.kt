package com.saveourtool.save.core.utils

import com.saveourtool.save.core.config.SaveOverrides
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals

class TomlUtilsTest {
    @Test
    fun `read save overrides`() {
        val testPath = "src/commonNonJsTest/resources/save-overrides.toml".toPath()
        val fix = readFromFile<SaveOverrides, SaveOverrides.SaveOverridesInterim>(
            testPath,
            "fix",
            ""
        )
        assertEquals("sh -a && source .env", fix.execCmd)
        assertEquals("--encode", fix.execFlags)
        assertEquals(1, fix.batchSize)
        assertEquals(", ", fix.batchSeparator)
        val warn = readFromFile<SaveOverrides, SaveOverrides.SaveOverridesInterim>(
            testPath,
            "warn",
            ""
        )
        assertEquals("sh -c", warn.execCmd)
        assertEquals("--warn", warn.execFlags)
        assertEquals(4, warn.batchSize)
        assertEquals(", ", warn.batchSeparator)
        val fixAndWarn = readFromFile<SaveOverrides, SaveOverrides.SaveOverridesInterim>(
            testPath,
            "fix and warn",
            ""
        )
        assertEquals(null, fixAndWarn.execCmd)
        assertEquals(null, fixAndWarn.execFlags)
        assertEquals(1, fixAndWarn.batchSize)
        assertEquals(", ", fixAndWarn.batchSeparator)
    }
}