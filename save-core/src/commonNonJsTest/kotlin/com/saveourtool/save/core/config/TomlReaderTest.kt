package com.saveourtool.save.core.config

import okio.Path.Companion.toPath
import kotlin.test.Test

class TomlReaderTest {
    @Test
    fun `general case`() {
        val saveOverrides: SaveOverrides = TomlReader().read("src/commonNonJsTest/resources/save-overrides.toml".toPath())
    }
}