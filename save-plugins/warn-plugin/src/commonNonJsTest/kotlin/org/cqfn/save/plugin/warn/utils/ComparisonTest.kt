package org.cqfn.save.plugin.warn.utils

import org.cqfn.save.core.files.createFile
import org.cqfn.save.plugin.warn.WarnPluginConfig

import okio.FileSystem

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

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
        val expectedWarningsMap: Map<String?, List<Warning>> = mapOf(
            ("filename") to listOf(
                Warning("foo", 8, 5, resourceFileName)
            )
        )
        val actualWarningsMap: Map<String?, List<Warning>> = mapOf(
            ("filename") to listOf(
                Warning("bar", 8, 5, resourceFileName),
                Warning("baz", 8, 5, resourceFileName),
                Warning("foo", 8, 5, resourceFileName)
            )
        )
        val warnPluginConfig = WarnPluginConfig(exactWarningsMatch = false)
        val config = fs.createFile(tmpDir / "save.toml")

        val testStatus = WarnPlugin(
            TestConfig(config, null, mutableListOf(warnPluginConfig), fs),
            testFiles = emptyList(),
            fs,
        )
            .checkResults(expectedWarningsMap["filename"]!!, actualWarningsMap["filename"]!!, warnPluginConfig)

        assertTrue(testStatus is Pass, "Actual type of status is ${testStatus::class}")
        assertEquals(
            "Some warnings were unexpected: ${actualWarningsMap.values.single().dropLast(1)}",
            testStatus.message)
    }
}
