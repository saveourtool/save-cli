package com.saveourtool.save.plugin.warn.utils

import com.saveourtool.save.core.files.createFile
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.plugin.warn.WarnPluginConfig

import okio.FileSystem

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComparisonTest {
    private val fs = FileSystem.SYSTEM
    private val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / ComparisonTest::class.simpleName!!)

    @BeforeTest
    fun setUp() {
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
        fs.createDirectory(tmpDir)
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }

    @Test
    @Suppress("TYPE_ALIAS")
    fun `should compare warnings`() {
        val resourceFileName = "resource"
        val expectedWarningsMap: Map<String, List<Warning>> = mapOf(
            ("filename") to listOf(
                Warning("foo", 8, 5, resourceFileName)
            )
        )
        val actualWarningsMap: Map<String, List<Warning>> = mapOf(
            ("filename") to listOf(
                Warning("bar", 8, 5, resourceFileName),
                Warning("baz", 8, 5, resourceFileName),
                Warning("foo", 8, 5, resourceFileName)
            )
        )
        val warnPluginConfig = WarnPluginConfig(exactWarningsMatch = false, patternForRegexInWarning = listOf("{{", "}}"))
        fs.createFile(tmpDir / "save.toml")

        val results = ResultsChecker(
            expectedWarningsMap, actualWarningsMap, warnPluginConfig
        ).checkResults("filename").first

        assertTrue(results is Pass, "Actual type of status is ${results::class}")
        assertEquals(
            "(UNEXPECTED WARNINGS): ${actualWarningsMap.values.single().dropLast(1)}",
            results.message)
    }
}
