package com.saveourtool.save.plugins.fix

import com.saveourtool.save.core.config.TestConfig
import com.saveourtool.save.core.files.createFile
import com.saveourtool.save.core.plugin.GeneralConfig
import com.saveourtool.save.core.result.DebugInfo
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.core.result.TestResult
import com.saveourtool.save.core.utils.ProcessBuilder
import com.saveourtool.save.core.utils.isCurrentOsWindows

import okio.FileSystem

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Needed tests:
 * - discovering of file pairs
 * - running tool
 */
class FixPluginTest {
    private val fs = FileSystem.SYSTEM
    private val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "${FixPluginTest::class.simpleName!!}-${Random.nextInt()}").also {
        fs.createDirectory(it)
    }

    @Test
    fun `should detect two files`() {
        fs.createFile(tmpDir / "save.toml")
        fs.createFile(tmpDir / "Test1Test.java")
        fs.createFile(tmpDir / "Test1Expected.java")

        val pairs = discoverFilePairs()

        assertEquals(1, pairs.size)
        assertEquals("Test1Test.java", pairs.single().first.name)
        assertEquals("Test1Expected.java", pairs.single().second.name)
    }
    @Test
    fun `should detect two files - among other files`() {
        fs.createFile(tmpDir / "save.toml")
        fs.createFile(tmpDir / "Test2Test.java")
        fs.createFile(tmpDir / "Test2Expected.java")
        fs.createFile(tmpDir / "Something.java")
        fs.createFile(tmpDir / "SomethingExpected.java")
        fs.createFile(tmpDir / "Anything.java")
        fs.createFile(tmpDir / "AnythingTest.java")
        fs.createFile(tmpDir / "CompletelyDifferentTest.java")
        fs.createFile(tmpDir / "NowCompletelyDifferentExpected.java")
        fs.createFile(tmpDir / "AndNowCompletelyDifferent.java")

        val pairs = discoverFilePairs()

        assertEquals(1, pairs.size)
        assertEquals("Test2Test.java", pairs.single().first.name)
        assertEquals("Test2Expected.java", pairs.single().second.name)
    }

    @Test
    fun `should calculate diff of discovered files`() {
        val config = fs.createFile(tmpDir / "save.toml")
        val testFile = fs.createFile(tmpDir / "Test3Test.java")
        fs.write(testFile) {
            write("Original file".encodeToByteArray())
        }
        val expectedFile = fs.createFile(tmpDir / "Test3Expected.java")
        fs.write(expectedFile) {
            write("Expected file".encodeToByteArray())
        }
        val diskWithTmpDir = if (isCurrentOsWindows()) "${tmpDir.toString().substringBefore("\\").lowercase()} && " else ""
        val executionCmd = "${diskWithTmpDir}cd $tmpDir && echo Expected file >"

        val fixPlugin = FixPlugin(TestConfig(config,
            null,
            mutableListOf(
                FixPluginConfig(executionCmd),
                GeneralConfig("", listOf(""), "", "")
            ), fs),
            testFiles = emptyList(),
            fs,
            useInternalRedirections = false
        )
        val results = fixPlugin.execute().toList()

        assertEquals(1, results.size, "Size of results should equal number of pairs")
        val testResult = results.single()
        assertEquals(
            TestResult(FixPlugin.FixTestFiles(testFile, expectedFile), Pass(null), DebugInfo(
                testResult.debugInfo?.execCmd, testResult.debugInfo?.stdout, testResult.debugInfo?.stderr, null)
            ),
            testResult
        )
        // FixMe: check that Test3Test.java in temporary directory is identical with expected file
    }

    @Test
    @Suppress("TOO_LONG_FUNCTION")
    fun `test for batchSize`() {
        val config = fs.createFile(tmpDir / "save.toml")
        val testFile1 = fs.createFile(tmpDir / "Test3Test.java")
        val testFile2 = fs.createFile(tmpDir / "Test4Test.java")
        fs.write(testFile1) {
            write("Original file".encodeToByteArray())
        }
        fs.write(testFile2) {
            write("Original file".encodeToByteArray())
        }
        val expectedFile1 = fs.createFile(tmpDir / "Test3Expected.java")
        val expectedFile2 = fs.createFile(tmpDir / "Test4Expected.java")
        fs.write(expectedFile1) {
            write("Expected file".encodeToByteArray())
        }
        fs.write(expectedFile2) {
            write("Expected file".encodeToByteArray())
        }
        val diskWithTmpDir = if (isCurrentOsWindows()) "${tmpDir.toString().substringBefore("\\").lowercase()} && " else ""
        // FixMe: after https://github.com/saveourtool/save/issues/158
        val executionCmd = if (isCurrentOsWindows()) {
            // We call ProcessBuilder ourselves, because the command ">" does not work for the list of files
            ProcessBuilder(false, fs).exec("echo Expected file > $testFile2", "", null, 10_000L)
            "${diskWithTmpDir}cd $tmpDir && echo Expected file >"
        } else {
            "${diskWithTmpDir}cd $tmpDir && echo Expected file | tee"
        }

        val fixPluginConfig = if (isCurrentOsWindows()) FixPluginConfig(executionCmd, 2) else FixPluginConfig(executionCmd, 2, " ")

        val fixPlugin = FixPlugin(TestConfig(config,
            null,
            mutableListOf(
                fixPluginConfig,
                GeneralConfig("", listOf(""), "", "")
            ), fs),
            testFiles = emptyList(),
            fs,
            useInternalRedirections = false
        )
        val results = fixPlugin.execute().toList()

        // We call ProcessBuilder ourselves, because the command ">" does not work for the list of files
        ProcessBuilder(false, fs).exec("echo Expected file > $testFile2", "", null, 10_000L)

        assertEquals(2, results.count(), "Size of results should equal number of pairs")
        assertTrue(results.all {
            it.status == Pass(null)
        })
        // FixMe: check that Test3Test.java and Test4Test.java in temporary directory are identical with expected files
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }

    internal fun discoverFilePairs() = FixPlugin(
        TestConfig(tmpDir / "save.toml", null, mutableListOf(FixPluginConfig("")), fs),
        testFiles = emptyList(),
        fs,
        useInternalRedirections = false
    )
        .discoverTestFiles(tmpDir)
        .map { it as FixPlugin.FixTestFiles }
        .map { it.test to it.expected }
        .toList()
}
