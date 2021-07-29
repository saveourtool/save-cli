package org.cqfn.save.plugins.fixandwarn

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.utils.isCurrentOsWindows
import org.cqfn.save.plugin.warn.WarnPluginConfig
import org.cqfn.save.plugins.fix.FixPlugin
import org.cqfn.save.plugins.fix.FixPluginConfig

import io.github.petertrr.diffutils.diff
import okio.FileSystem

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FixAndWarnPluginTest {
    private val fs = FileSystem.SYSTEM
    private val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "${FixAndWarnPluginTest::class.simpleName!!}-${Random.nextInt()}")

    @BeforeTest
    fun setUp() {
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
        fs.createDirectory(tmpDir)
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `base test`() {
        val config = fs.createFile(tmpDir / "save.toml")

        val textBeforeFixAndWarn =
                """
                    package org.cqfn.save.example
                    
                    class example {
                        int foo = 42;
                    }
                """.trimIndent()

        val testFile = fs.createFile(tmpDir / "Test1Test.java")
        fs.write(testFile) {
            write(textBeforeFixAndWarn.encodeToByteArray())
        }

        val textAfterFixAndWarn =
                """
                    package org.cqfn.save.example
                    
                    // ;warn:4:6: Some Warning
                    class Example {
                        int foo = 42;
                    }
                """.trimIndent()

        val expectedFile = fs.createFile(tmpDir / "Test1Expected.java")
        fs.write(expectedFile) {
            write(textAfterFixAndWarn.encodeToByteArray())
        }

        val diskWithTmpDir = if (isCurrentOsWindows()) "${tmpDir.toString().substringBefore("\\").lowercase()} && " else ""
        val catCmd = if (isCurrentOsWindows()) "type" else "cat"
        val fixExecutionCmd = "${diskWithTmpDir}cd $tmpDir && $catCmd $expectedFile >"
        val warnExecutionCmd = "echo Test1Expected.java:4:6: Some Warning && set stub="

        val results = FixAndWarnPlugin(
            TestConfig(
                config,
                null,
                mutableListOf(
                    FixAndWarnPluginConfig(
                        FixPluginConfig(fixExecutionCmd, batchSize = 1),
                        WarnPluginConfig(warnExecutionCmd,
                            Regex("(.+):(\\d+):(\\d+): (.+)"),
                            true, true, 1, ", ", 1, 2, 3, 1, 2, 3, 4
                        )
                    ),
                    GeneralConfig("", "", "", "", expectedWarningsPattern = Regex("// ;warn:(\\d+):(\\d+): (.*)"))
                ),
                fs,
            ),
            testFiles = emptyList(),
            fs,
            useInternalRedirections = false
        ).execute().toList()

        println("Results ${results.toList()}")
        assertEquals(1, results.count(), "Size of results should equal number of pairs")
        // Check FixPlugin results
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / FixPlugin::class.simpleName!!)
        assertTrue("Files should be identical") {
            // Additionally ignore warnings in expected file
            diff(fs.readLines(tmpDir / "Test1Test.java"), fs.readLines(expectedFile).filterNot { it.contains("warn") })
                .deltas.isEmpty()
        }
        // Check WarnPlugin results
        assertTrue(results.single().status is Pass)
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }
}
